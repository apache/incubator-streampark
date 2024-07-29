/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.console.core.service.alert.impl;

import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.base.util.FreemarkerUtils;
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.bean.EmailConfig;
import org.apache.streampark.console.core.service.alert.AlertNotifyService;

import org.apache.commons.mail.HtmlEmail;

import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Lazy
public class EmailAlertNotifyServiceImpl implements AlertNotifyService {

    private final Template template = FreemarkerUtils.loadTemplateFile("alert-email.ftl");

    @Override
    public boolean doAlert(AlertConfigParams alertConfig, AlertTemplate template) throws AlertException {
        EmailConfig emailConfig = Optional.ofNullable(EmailConfig.fromSetting())
            .orElseThrow(() -> new AlertException("Please configure the email sender first"));
        String contacts = alertConfig.getEmailParams() == null ? null : alertConfig.getEmailParams().getContacts();
        if (!StringUtils.hasLength(contacts)) {
            throw new AlertException("Please configure a valid contacts");
        }
        String[] emails = contacts.split(",");
        return sendEmail(emailConfig, template, emails);
    }

    private boolean sendEmail(EmailConfig emailConfig, AlertTemplate mail, String... mails) throws AlertException {
        try {
            Map<String, AlertTemplate> out = new HashMap<>(16);
            out.put("mail", mail);
            String html = FreemarkerUtils.format(template, out);

            HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setCharset("UTF-8");
            htmlEmail.setHostName(emailConfig.getSmtpHost());
            htmlEmail.setAuthentication(emailConfig.getUserName(), emailConfig.getPassword());
            htmlEmail.setFrom(emailConfig.getFrom());
            if (emailConfig.isSsl()) {
                htmlEmail.setSSLCheckServerIdentity(true);
                htmlEmail.setSSLOnConnect(true);
                htmlEmail.setSslSmtpPort(emailConfig.getSmtpPort().toString());
            } else {
                htmlEmail.setSmtpPort(emailConfig.getSmtpPort());
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
