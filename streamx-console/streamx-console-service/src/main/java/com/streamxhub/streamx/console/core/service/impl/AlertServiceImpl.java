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

import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.util.WebUtils;
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/**
 * @author benjobs
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    private static final String CONFIG_TEMPLATE = "email.html";

    private Template template;

    @Autowired
    private SettingService settingService;

    private SenderEmail senderEmail;

    @PostConstruct
    public void initConfig() throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (Objects.isNull(this.template = loadingConf(configuration))){
            Enumeration<URL> urls = ClassLoader.getSystemResources(CONFIG_TEMPLATE);
            if (urls != null) {
                if (!urls.hasMoreElements()) {
                    urls = Thread.currentThread().getContextClassLoader().getResources(CONFIG_TEMPLATE);
                }

                if (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    if (url.getPath().contains(".jar")) {
                        configuration.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "");
                    } else {
                        File file = new File(url.getPath());
                        configuration.setDirectoryForTemplateLoading(file.getParentFile());
                    }
                    this.template = configuration.getTemplate(CONFIG_TEMPLATE);
                }
            } else {
                log.error("email.html not found!");
                throw new ExceptionInInitializerError("email.html not found!");
            }
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
            mail.setTitle(String.format("Notify: %s %s", application.getJobName(), appState.name()));
            mail.setStatus(appState.name());

            String subject = String.format("StreamX Alert: %s %s", application.getJobName(), appState.name());
            String[] emails = application.getAlertEmail().split(",");
            sendEmail(mail, subject, emails);
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
                htmlEmail.setSSLOnConnect(true).setSslSmtpPort(this.senderEmail.getSmtpPort().toString());
            } else {
                htmlEmail.setSmtpPort(this.senderEmail.getSmtpPort());
            }
            htmlEmail.setSubject(subject);
            htmlEmail.setHtmlMsg(html);
            htmlEmail.addTo(mails);
            htmlEmail.send();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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

        // TODO: modify url for both k8s and yarn execute mode, the k8s mode is different from yarn, when the flink job failed ,
        //  the k8s pod is missing , so we should look for  a more reasonable url for k8s
        String url = "";
        if (ExecutionMode.isYarnMode(application.getExecutionMode())) {
            String format = "%s/proxy/%s/";
            url = String.format(format, HadoopUtils.getRMWebAppURL(false), application.getAppId());
        }

        MailTemplate template = new MailTemplate();
        template.setJobName(application.getJobName());
        template.setLink(url);
        template.setStartTime(DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setEndTime(DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(),
            DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setDuration(DateUtils.toRichTimeDuration(duration));
        boolean needRestart = application.isNeedRestartOnFailed() && application.getRestartCount() > 0;
        template.setRestart(needRestart);
        if (needRestart) {
            template.setRestartIndex(application.getRestartCount());
            template.setTotalRestart(application.getRestartSize());
        }
        return template;
    }

    private Template loadingConf(Configuration configuration) {
        try {
            File file = WebUtils.getAppConfDir();
            log.info("loading email config... dir :{}", file.getPath());
            configuration.setDirectoryForTemplateLoading(file);
            return configuration.getTemplate(CONFIG_TEMPLATE);
        } catch (IOException e){
            log.warn("loading email error :{}", e.getMessage());
        }
        return null;
    }

}
