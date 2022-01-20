/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.entity.SenderSMS;
import com.streamxhub.streamx.console.core.enums.CheckPointStatus;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.metrics.flink.MailTemplate;
import com.streamxhub.streamx.console.core.metrics.flink.SMSTemplate;
import com.streamxhub.streamx.console.core.service.AlertService;
import com.streamxhub.streamx.console.core.service.SettingService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.HtmlEmail;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author benjobs
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    private Template template;

    @Autowired
    private SettingService settingService;

    private SenderEmail senderEmail;

    private SenderSMS senderSMS;

    @PostConstruct
    public void initConfig() throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        String template = "email.html";
        Enumeration<URL> urls = ClassLoader.getSystemResources(template);
        if (urls != null) {
            if (!urls.hasMoreElements()) {
                urls = Thread.currentThread().getContextClassLoader().getResources(template);
            }

            if (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getPath().contains(".jar")) {
                    configuration.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "");
                } else {
                    File file = new File(url.getPath());
                    configuration.setDirectoryForTemplateLoading(file.getParentFile());
                }
                configuration.setDefaultEncoding("UTF-8");
                this.template = configuration.getTemplate(template);
            }
        } else {
            log.error("email.html not found!");
            throw new ExceptionInInitializerError("email.html not found!");
        }
    }

    @Override
    public void alert(Application application, FlinkAppState appState) {
        if (this.senderEmail == null) {
            this.senderEmail = settingService.getSenderEmail();
        }
        if (this.senderEmail != null && Utils.notEmpty(application.getAlertEmail())) {
            MailTemplate mail = getMailTemplate(application);
            mail.setType(1);
            mail.setTitle(String.format("通知:%s已经%s", application.getJobName(), appState.name()));
            mail.setStatus(appState.name());

            String subject = String.format("实时计算任务:%s已经%s", application.getJobName(), appState.name());
            String[] emails = application.getAlertEmail().split(",");
            sendEmail(mail, subject, emails);
        }
        if (this.senderSMS == null) {
            this.senderSMS = settingService.getSenderSMS();
        }
        if (Utils.notEmpty(this.senderSMS.getPhoneHost()) &&
            (Utils.notEmpty(this.senderSMS.getPhoneNumber()) || Utils.notEmpty(application.getAlertPhoneNumber()))) {
            String phoneHost = this.senderSMS.getPhoneHost();
            SMSTemplate sms = getSMSTemplate(application);
            sms.setType(1);
            sms.setTitle(String.format("通知:%s已经%s", application.getJobName(), appState.name()));
            sms.setStatus(appState.name());
            String subject = String.format("实时计算任务:%s已经%s", application.getJobName(), appState.name());
            String phoneNumber = this.senderSMS.getPhoneNumber();
            if(Utils.notEmpty(application.getAlertPhoneNumber()))
            {
                phoneNumber = application.getAlertPhoneNumber();
            }
            sendSMS(sms, subject, phoneNumber,phoneHost);
        }
    }

    @Override
    public void alert(Application application, CheckPointStatus checkPointStatus) {
        if (this.senderEmail == null) {
            this.senderEmail = settingService.getSenderEmail();
        }
        if (this.senderEmail != null && Utils.notEmpty(application.getAlertEmail())) {
            MailTemplate mail = getMailTemplate(application);
            mail.setType(2);
            mail.setCpFailureRateInterval(DateUtils.toRichTimeDuration(application.getCpFailureRateInterval()));
            mail.setCpMaxFailureInterval(application.getCpMaxFailureInterval());
            mail.setTitle(String.format("通知: %s checkpoint 失败", application.getJobName()));

            String subject = String.format("实时计算任务: %s, checkPoint 失败", application.getJobName());
            String[] emails = application.getAlertEmail().split(",");
            sendEmail(mail, subject, emails);
        }
        if (this.senderSMS == null) {
            this.senderSMS = settingService.getSenderSMS();
        }
        if (Utils.notEmpty(this.senderSMS.getPhoneHost()) &&
            (Utils.notEmpty(this.senderSMS.getPhoneNumber()) || Utils.notEmpty(application.getAlertPhoneNumber()))) {
            String phoneHost = this.senderSMS.getPhoneHost();
            SMSTemplate smsTemplate = getSMSTemplate(application);
            smsTemplate.setType(2);
            smsTemplate.setCpFailureRateInterval(DateUtils.toRichTimeDuration(application.getCpFailureRateInterval()));
            smsTemplate.setCpMaxFailureInterval(application.getCpMaxFailureInterval());
            smsTemplate.setTitle(String.format("通知: %s checkpoint 失败", application.getJobName()));
            String subject = String.format("实时计算任务: %s, checkPoint 失败", application.getJobName());
            String phoneNumber = this.senderSMS.getPhoneNumber();
            if(Utils.notEmpty(application.getAlertPhoneNumber()))
            {
                phoneNumber = application.getAlertPhoneNumber();
            }
            sendSMS(smsTemplate, subject, phoneNumber,phoneHost);
        }
    }

    private void sendEmail(MailTemplate mail, String subject, String... mails) {
        log.info(subject);
        try {
            Map<String, MailTemplate> out = new HashMap<>(16);
            out.put("mail", mail);

            StringWriter writer = new StringWriter();
            template.process(out, writer);
            String html = writer.toString();
            writer.close();

            HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setCharset("UTF-8");
            htmlEmail.setHostName(this.senderEmail.getSmtpHost());
            htmlEmail.setAuthentication(this.senderEmail.getUserName(), this.senderEmail.getPassword());
            htmlEmail.setFrom(this.senderEmail.getFrom());
            if (this.senderEmail.isSsl()) {
                htmlEmail.setSSLOnConnect(true);
                htmlEmail.setSslSmtpPort(this.senderEmail.getSmtpPort().toString());
            } else {
                htmlEmail.setSmtpPort(this.senderEmail.getSmtpPort());
            }
            htmlEmail.setSubject(subject);
            htmlEmail.setHtmlMsg(html);
            htmlEmail.addTo(mails);
            htmlEmail.send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String sendSMS(SMSTemplate sms, String subject, String mobile,String phoneHost) {
        log.info(subject);
        String response = null;
        String jobName = sms.getJobName();
        String startTime = sms.getStartTime();
        String endTime = sms.getEndTime();
        String link = sms.getLink();
        String alertMessage = subject + "\r\n"
             +"实时任务结束于 " + endTime
            + ",任务详情请查看" + "\r\n"
            + link;
        log.info("alertMessage = " + alertMessage);
        try {
            CloseableHttpClient httpclient = null;
            CloseableHttpResponse httpresponse = null;
            try {
                httpclient = HttpClients.createDefault();
                HttpPost httppost = new HttpPost(phoneHost);
                JSONObject jsonData = new JSONObject();
                jsonData.put("mobile",mobile);
                jsonData.put("content",alertMessage);
                jsonData.put("system","streamx");
                jsonData.put("businessKey",jobName+"_"+startTime);
                StringEntity stringentity = new StringEntity(jsonData.toString(), ContentType.create("application/json", "UTF-8"));
                httppost.setEntity(stringentity);
                //发post请求
                httpresponse = httpclient.execute(httppost);
                //utf-8参数防止中文乱码
                response = EntityUtils.toString(httpresponse.getEntity(), "utf-8");
            } finally {
                if (httpclient != null) {
                    httpclient.close();
                }
                if (httpresponse != null) {
                    httpresponse.close();
                }
            }
        } catch (Exception e) {
            log.error("sms send post http is error!",e);
            e.printStackTrace();
        }
        return response;
    }

    private MailTemplate getMailTemplate(Application application) {
        long duration;
        if (application.getEndTime() == null) {
            duration = System.currentTimeMillis() - application.getStartTime().getTime();
        } else {
            duration = application.getEndTime().getTime() - application.getStartTime().getTime();
        }
        duration = duration / 1000 / 60;
        String format = "%s/proxy/%s/";
        String url = String.format(format, HadoopUtils.getRMWebAppURL(false), application.getAppId());

        MailTemplate template = new MailTemplate();
        template.setJobName(application.getJobName());
        template.setLink(url);
        template.setStartTime(DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setEndTime(DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setDuration(DateUtils.toRichTimeDuration(duration));
        boolean needRestart = application.isNeedRestartOnFailed() && application.getRestartCount() > 0;
        template.setRestart(needRestart);
        if (needRestart) {
            template.setRestartIndex(application.getRestartCount());
            template.setTotalRestart(application.getRestartSize());
        }
        return template;
    }
    private SMSTemplate getSMSTemplate(Application application) {
        long duration;
        if (application.getEndTime() == null) {
            duration = System.currentTimeMillis() - application.getStartTime().getTime();
        } else {
            duration = application.getEndTime().getTime() - application.getStartTime().getTime();
        }
        duration = duration / 1000 / 60;
        String format = "%s/proxy/%s/";
        String url = String.format(format, HadoopUtils.getRMWebAppURL(false), application.getAppId());

        SMSTemplate template = new SMSTemplate();
        template.setJobName(application.getJobName());
        template.setLink(url);
        template.setStartTime(DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setEndTime(DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setDuration(DateUtils.toRichTimeDuration(duration));
        boolean needRestart = application.isNeedRestartOnFailed() && application.getRestartCount() > 0;
        template.setRestart(needRestart);
        if (needRestart) {
            template.setRestartIndex(application.getRestartCount());
            template.setTotalRestart(application.getRestartSize());
        }
        return template;
    }

}
