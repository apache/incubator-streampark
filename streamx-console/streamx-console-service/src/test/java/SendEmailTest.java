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

import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

public class SendEmailTest {

    private Template template;

    private SenderEmail senderEmail;

    @Before
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
            throw new ExceptionInInitializerError("email.html not found!");
        }
        senderEmail = new SenderEmail();
        senderEmail.setEmail("******");
        senderEmail.setPassword("******");
        senderEmail.setSmtpPort(465);
        senderEmail.setSsl(true);
        senderEmail.setSmtpHost("smtp.exmail.qq.com");
    }


    @Test
    public void alert() {
        Application application = new Application();
        application.setStartTime(new Date());
        application.setJobName("Test My Job");
        application.setAppId("1234567890");
        application.setAlertEmail("******");

        application.setCpFailureAction(1);
        application.setCpFailureRateInterval(30);
        application.setCpMaxFailureInterval(5);
        if (Utils.notEmpty(application.getAlertEmail())) {
            try {
                Map<String, String> root = getAlertBaseInfo(application);
                root.put("title", "Notify: " + application.getJobName().concat(" checkpoint FAILED"));
                root.put("jobDisplay", "none");
                root.put("savePointDisplay", "block");
                root.put("cpFailureRateInterval", DateUtils.toRichTimeDuration(application.getCpFailureRateInterval()));
                root.put("cpMaxFailureInterval", application.getCpMaxFailureInterval().toString());
                root.put("status", "FAILED");
                root.put("restartIndex", "2");
                root.put("totalRestart", "100");

                StringWriter writer = new StringWriter();
                template.process(root, writer);
                String html = writer.toString();
                writer.close();

                String subject = String.format("StreamX Alert: %s, checkPoint is Failed", application.getJobName());
                sendEmail(subject, html, application.getAlertEmail().split(","));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> getAlertBaseInfo(Application application) {
        long duration;
        if (application.getEndTime() == null) {
            duration = System.currentTimeMillis() - application.getStartTime().getTime();
        } else {
            duration = application.getEndTime().getTime() - application.getStartTime().getTime();
        }
        duration = duration / 1000 / 60;
        String format = "%s/proxy/%s/";
        String url = String.format(format, HadoopUtils.getRMWebAppURL(false), application.getAppId());

        Map<String, String> root = new HashMap<>();
        root.put("jobName", application.getJobName());
        root.put("startTime", DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        root.put("endTime", DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        root.put("duration", DateUtils.toRichTimeDuration(duration));
        root.put("link", url);
        return root;
    }

    private void sendEmail(String subject, String html, String... mails) throws EmailException {
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
        htmlEmail.setHtmlMsg(html);
        htmlEmail.addTo(mails);
        htmlEmail.send();
    }

}
