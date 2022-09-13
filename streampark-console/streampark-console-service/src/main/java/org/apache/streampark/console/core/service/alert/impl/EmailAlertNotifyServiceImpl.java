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
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.bean.SenderEmail;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.alert.AlertNotifyService;

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
import java.util.Optional;

@Slf4j
@Service
@Lazy
public class EmailAlertNotifyServiceImpl implements AlertNotifyService {

    private Template template;

    @Autowired
    private SettingService settingService;

    @PostConstruct
    public void loadTemplateFile() throws Exception {
        String template = "alert-email.ftl";
        this.template = FreemarkerUtils.loadTemplateFile(template);
    }

    @Override
    public boolean doAlert(AlertConfigWithParams alertConfig, AlertTemplate template) throws AlertException {
        SenderEmail senderEmail = Optional.ofNullable(settingService.getSenderEmail())
            .orElseThrow(() -> new AlertException("Please configure first mail sender"));
        String contacts = alertConfig.getEmailParams() == null ? null : alertConfig.getEmailParams().getContacts();
        if (!StringUtils.hasLength(contacts)) {
            throw new AlertException("Please configure a valid contacts");
        }
        String[] emails = contacts.split(",");
        return sendEmail(senderEmail, template, emails);
    }

    private boolean sendEmail(SenderEmail senderEmail, AlertTemplate mail, String... mails) throws AlertException {
        log.info(mail.getSubject());
        try {
            Map<String, AlertTemplate> out = new HashMap<>(16);
            out.put("mail", mail);
            String html = FreemarkerUtils.format(template, out);

            HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setCharset("UTF-8");
            htmlEmail.setHostName(senderEmail.getSmtpHost());
            htmlEmail.setAuthentication(senderEmail.getUserName(), senderEmail.getPassword());
            htmlEmail.setFrom(senderEmail.getFrom());
            if (senderEmail.isSsl()) {
                htmlEmail.setSSLOnConnect(true);
                htmlEmail.setSslSmtpPort(senderEmail.getSmtpPort().toString());
            } else {
                htmlEmail.setSmtpPort(senderEmail.getSmtpPort());
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
