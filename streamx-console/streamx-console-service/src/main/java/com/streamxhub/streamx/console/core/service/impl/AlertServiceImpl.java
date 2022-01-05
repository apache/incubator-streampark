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

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.properties.DingdingProperties;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.enums.CheckPointStatus;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.metrics.flink.MailTemplate;
import com.streamxhub.streamx.console.core.service.AlertService;
import com.streamxhub.streamx.console.core.service.SettingService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author benjobs
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {
    @Autowired
    private static DingdingProperties dingdingProperties;

    private Template template;

    @Autowired
    private SettingService settingService;

    private SenderEmail senderEmail;

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
        sendDing(application);
        if (this.senderEmail == null) {
            this.senderEmail = settingService.getSenderEmail();
        }
        if (this.senderEmail != null && Utils.notEmpty(application.getAlertEmail())) {
            MailTemplate mail = getMailTemplate(application);
            mail.setType(1);
            mail.setTitle(String.format("Notify: %s %s", application.getJobName(), appState.name()));
            mail.setStatus(appState.name());

            String subject = String.format("StreamX Alert: %s %s", application.getJobName(), appState.name());
            String[] emails = application.getAlertEmail().split(",");
            sendEmail(mail, subject, emails);
        }
    }

    @Override
    public void alert(Application application, CheckPointStatus checkPointStatus) {
        sendDing(application);
        if (this.senderEmail == null) {
            this.senderEmail = settingService.getSenderEmail();
        }
        if (this.senderEmail != null && Utils.notEmpty(application.getAlertEmail())) {
            MailTemplate mail = getMailTemplate(application);
            mail.setType(2);
            mail.setCpFailureRateInterval(DateUtils.toRichTimeDuration(application.getCpFailureRateInterval()));
            mail.setCpMaxFailureInterval(application.getCpMaxFailureInterval());
            mail.setTitle(String.format("Notify: %s checkpoint FAILED", application.getJobName()));

            String subject = String.format("StreamX Alert: %s, checkPoint is Failed", application.getJobName());
            String[] emails = application.getAlertEmail().split(",");
            sendEmail(mail, subject, emails);
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

    /**
     * dingding  alert
     * @param application
     * @throws Exception
     */
    private void sendDing(Application application){
        try {
            if (dingdingProperties.isEnabled() && application != null) {
                String content = "StreamX >>>>>>>>> ID:" + application.getId() + ",JOB NAME:" + application.getJobName() + "执行失败！" + "SavePointed:" + application.getSavePointed() + " SavePoint:" + application.getSavePoint();
                Long timestamp = System.currentTimeMillis();
                String secret = dingdingProperties.getSecret();
                String stringToSign = timestamp + "\n" + secret;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
                byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
                String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
                String url = String.format(dingdingProperties.getUrl(), dingdingProperties.getAccessToken(), timestamp, sign);
                DingTalkClient client = new DefaultDingTalkClient(url);
                OapiRobotSendRequest request = new OapiRobotSendRequest();
                request.setMsgtype("text");
                OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
                text.setContent(content);
                request.setText(text);
                OapiRobotSendResponse response = client.execute(request);
                if (response.isSuccess()) {
                    log.info("Send dingding success, msg = {}", content);
                } else {
                    log.error("Send dingding fail , errorMsg = {},content = {}", response.getErrmsg(), content);
                }
            } else {
                log.info("Send dingding  enabled is false,Please set streamx.dingding.enabled is true if you want to send dingding alert msg !");
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }

}
