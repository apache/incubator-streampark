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

package com.streamxhub.streamx.console.core.service.alert.impl;

import com.streamxhub.streamx.console.base.exception.AlertException;
import com.streamxhub.streamx.console.base.util.FreemarkerUtils;
import com.streamxhub.streamx.console.core.entity.SenderEmail;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;
import com.streamxhub.streamx.console.core.entity.alert.AlertTemplate;
import com.streamxhub.streamx.console.core.service.SettingService;
import com.streamxhub.streamx.console.core.service.alert.AlertNotifyService;

import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
@Slf4j
@Service
@Lazy
public class EmailAlertNotifyServiceImpl implements AlertNotifyService {

    private Template template;

    @Autowired
    private SettingService settingService;

    private SenderEmail senderEmail;

    @PostConstruct
    public void loadTemplateFile() throws Exception {
        String template = "alert-email.ftl";
        this.template = FreemarkerUtils.loadTemplateFile(template);
    }

    @Override
    public boolean doAlert(AlertConfigWithParams alertConfig, AlertTemplate template) throws AlertException {
        if (this.senderEmail == null) {
            this.senderEmail = settingService.getSenderEmail();
        }
        if (this.senderEmail == null) {
            throw new AlertException("Please configure first mail sender");
        }
        String contacts = alertConfig.getEmailParams() == null ? null : alertConfig.getEmailParams().getContacts();
        if (!StringUtils.hasLength(contacts)) {
            throw new AlertException("Please configure a valid contacts");
        }
        String[] emails = contacts.split(",");
        return sendEmail(template, emails);
    }

    private boolean sendEmail(AlertTemplate mail, String... mails) throws AlertException {
        log.info(mail.getSubject());
        try {
            Map<String, AlertTemplate> out = new HashMap<>(16);
            out.put("mail", mail);
            String html = FreemarkerUtils.format(template, out);

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
            htmlEmail.setSubject(mail.getSubject());
            htmlEmail.setHtmlMsg(html);
            htmlEmail.addTo(mails);
            htmlEmail.send();
            return true;
        } catch (Exception e) {
            throw new AlertException("Failed send email alert", e);
        }
    }

}
