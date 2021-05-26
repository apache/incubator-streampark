/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.enums.CheckPointStatus;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
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
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

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

    @PostConstruct
    public void initConfig() throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        String template = "email.html";
        Enumeration<URL> urls = ClassLoader.getSystemResources(template);
        if (urls != null) {
            if (!urls.hasMoreElements()) {
                urls = Thread.currentThread().getContextClassLoader().getResources(template);
            }
        }
        if (urls != null) {
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
    public void alert(Application application, Serializable state) {
        String subject = "StreamX Alert: {}, %s";
        if (state instanceof FlinkAppState) {
            subject = String.format(subject, application.getJobName(), "checkPoint is Failed");
        } else {
            subject = String.format(subject, application.getJobName(), " is ".concat(((CheckPointStatus) state).name()));
        }
        log.info(subject);
        if (this.senderEmail == null) {
            this.senderEmail = settingService.getSenderEmail();
        }
        if (this.senderEmail != null) {
            //发送邮件
            if (Utils.notEmpty(application.getAlertEmail())) {
                try {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setCharset("UTF-8");
                    htmlEmail.setHostName(this.senderEmail.getSmtpHost());
                    htmlEmail.setAuthentication(this.senderEmail.getEmail(), this.senderEmail.getPassword());
                    htmlEmail.setFrom(this.senderEmail.getEmail());
                    if (this.senderEmail.isSsl()) {
                        htmlEmail.setSSLOnConnect(true);
                        htmlEmail.setSslSmtpPort(this.senderEmail.getSmtpPort().toString());
                    }
                    htmlEmail.setSubject(subject);
                    String html;
                    if (state instanceof FlinkAppState) {
                        html = getHtmlMessage(application, (FlinkAppState) state);
                    } else {
                        html = getHtmlMessage(application, (CheckPointStatus) state);
                    }
                    htmlEmail.setHtmlMsg(html);
                    htmlEmail.addTo(application.getAlertEmail().split(","));
                    htmlEmail.send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getHtmlMessage(Application application, FlinkAppState appState) throws Exception {
        long duration;
        if (application.getEndTime() == null) {
            duration = System.currentTimeMillis() - application.getStartTime().getTime();
        } else {
            duration = application.getEndTime().getTime() - application.getStartTime().getTime();
        }
        duration = duration / 1000 / 60;
        String content = "Job [" + application.getJobName() + "] is " + appState.name() + "<br>" +
                "Start Time: " + DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()) + "<br>" +
                "End Time: " + DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(), DateUtils.fullFormat(), TimeZone.getDefault()) + "<br>" +
                "Duration: " + DateUtils.toRichTimeDuration(duration) + "<br>";

        if (appState.equals(FlinkAppState.FAILED)) {
            if (application.getRestartCount() > 0) {
                content += "Restart: <strong><span style='color:red'>" + application.getRestartCount() + "</span>/" + application.getRestartSize() + "</strong><br><br>";
            }
        }
        content += "please check it,Thank you for using StreamX<br><br>Best Wishes!!";

        Map<String, String> root = new HashMap<>(3);
        root.put("title", "Notify :" + application.getJobName().concat(" is ").concat(appState.name()));
        root.put("message", content);
        String format = "%s/proxy/%s/";
        String url = String.format(format, HadoopUtils.getRMWebAppURL(false), application.getAppId());
        root.put("link", url);

        StringWriter writer = new StringWriter();
        template.process(root, writer);
        return writer.toString();
    }

    private String getHtmlMessage(Application application, CheckPointStatus appState) throws Exception {
        long duration;
        if (application.getEndTime() == null) {
            duration = System.currentTimeMillis() - application.getStartTime().getTime();
        } else {
            duration = application.getEndTime().getTime() - application.getStartTime().getTime();
        }
        duration = duration / 1000 / 60;
        String content = "Job [" + application.getJobName() + "] checkPoint is " + appState.name() + "<br>" +
                "Start Time: " + DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()) + "<br>" +
                "End Time: " + DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(), DateUtils.fullFormat(), TimeZone.getDefault()) + "<br>" +
                "Duration: " + DateUtils.toRichTimeDuration(duration) + "<br>";

        content += "please check it,Thank you for using StreamX<br><br>Best Wishes!!";

        Map<String, String> root = new HashMap<>(3);
        root.put("title", "Notify :" + application.getJobName().concat(" checkpoint is ").concat(appState.name()));
        root.put("message", content);
        String format = "%s/proxy/%s/";
        String url = String.format(format, HadoopUtils.getRMWebAppURL(false), application.getAppId());
        root.put("link", url);

        StringWriter writer = new StringWriter();
        template.process(root, writer);
        return writer.toString();
    }

}
