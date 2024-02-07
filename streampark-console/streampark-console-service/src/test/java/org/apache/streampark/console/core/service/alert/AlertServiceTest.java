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

package org.apache.streampark.console.core.service.alert;

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.FreemarkerUtils;
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.bean.AlertDingTalkParams;
import org.apache.streampark.console.core.bean.AlertLarkParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.bean.AlertWeComParams;
import org.apache.streampark.console.core.bean.SenderEmail;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.service.alert.impl.DingTalkAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.LarkAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.WeComAlertNotifyServiceImpl;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Slf4j
@Disabled("These test cases can't be runnable due to external service is not available.")
class AlertServiceTest {
  AlertTemplate alertTemplate;
  AlertConfigWithParams params = new AlertConfigWithParams();
  ObjectMapper mapper = new ObjectMapper();
  RestTemplate restTemplate = new RestTemplate();
  private Template template;

  private SenderEmail senderEmail;

  @BeforeEach
  void before1() {
    initAlertTemplate();

    initConfigForSendEmail();
  }

  private void initAlertTemplate() {
    alertTemplate = new AlertTemplate();
    alertTemplate.setTitle("Notify: StreamPark alert job for test");
    alertTemplate.setSubject("StreamPark Alert: test-job OTHER");
    alertTemplate.setJobName("StreamPark alert job for test");
    alertTemplate.setLink("http://127.0.0.1:8080");
    alertTemplate.setStatus("TEST");
    alertTemplate.setType(1);
    alertTemplate.setRestart(true);
    alertTemplate.setTotalRestart(5);
    alertTemplate.setRestartIndex(2);
    Date date = new Date();
    alertTemplate.setStartTime(
        DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
    alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
    alertTemplate.setDuration("-");
  }

  void initConfigForSendEmail() {
    this.template = FreemarkerUtils.loadTemplateFile("alert-email.ftl");
    senderEmail = new SenderEmail();
    senderEmail.setFrom("****@domain.com");
    senderEmail.setUserName("******");
    senderEmail.setPassword("******");
    senderEmail.setSmtpPort(465);
    senderEmail.setSsl(true);
    senderEmail.setSmtpHost("smtp.exmail.qq.com");
  }

  void before2() {
    alertTemplate = new AlertTemplate();
    alertTemplate.setTitle("Alert: StreamPark alert job for test");
    alertTemplate.setSubject("StreamPark Alert: test-job OTHER");
    alertTemplate.setJobName("StreamPark alert job for test");
    alertTemplate.setLink("http://127.0.0.1:8080");
    alertTemplate.setStatus("TEST");
    alertTemplate.setType(2);
    alertTemplate.setCpMaxFailureInterval(5);
    alertTemplate.setCpFailureRateInterval("10%");
    Date date = new Date();
    alertTemplate.setStartTime(
        DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
    alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
    alertTemplate.setDuration("-");
  }

  @Test
  void testDingTalkAlert() throws Exception {
    DingTalkAlertNotifyServiceImpl notifyService = new DingTalkAlertNotifyServiceImpl(restTemplate);

    notifyService.loadTemplateFile();
    AlertDingTalkParams dingTalkParams = new AlertDingTalkParams();
    dingTalkParams.setToken("your_token");
    dingTalkParams.setContacts("175xxxx1234");
    dingTalkParams.setIsAtAll(true);

    params.setAlertType(2);
    params.setDingTalkParams(dingTalkParams);

    notifyService.doAlert(params, alertTemplate);
  }

  @Test
  void testWeComAlert() throws Exception {
    WeComAlertNotifyServiceImpl notifyService = new WeComAlertNotifyServiceImpl(restTemplate);
    notifyService.loadTemplateFile();

    AlertWeComParams weComParams = new AlertWeComParams();
    weComParams.setToken("your_token");

    params.setAlertType(4);
    params.setWeComParams(weComParams);

    notifyService.doAlert(params, alertTemplate);
  }

  @Test
  void testLarkAlert() {
    LarkAlertNotifyServiceImpl notifyService = new LarkAlertNotifyServiceImpl(restTemplate, mapper);
    notifyService.loadTemplateFile();

    AlertLarkParams alertLarkParams = new AlertLarkParams();
    alertLarkParams.setToken("your_token");

    params.setAlertType(16);
    params.setLarkParams(alertLarkParams);

    notifyService.doAlert(params, alertTemplate);
  }

  @Test
  void testAlert() {
    Application application = new Application();
    application.setStartTime(new Date());
    application.setJobName("Test My Job");
    application.setAppId("1234567890");
    application.setAlertId(1);

    application.setRestartCount(5);
    application.setRestartSize(100);

    application.setCpFailureAction(1);
    application.setCpFailureRateInterval(30);
    application.setCpMaxFailureInterval(5);

    FlinkAppState appState = FlinkAppState.FAILED;

    try {
      AlertTemplate mail = getAlertBaseInfo(application);
      mail.setType(1);
      mail.setTitle("Notify: " + application.getJobName().concat(" " + appState.name()));
      mail.setStatus(appState.name());

      StringWriter writer = new StringWriter();
      Map<String, AlertTemplate> out = new HashMap<>();
      out.put("mail", mail);

      template.process(out, writer);
      String html = writer.toString();
      log.info(html);
      writer.close();

      String subject =
          String.format("StreamPark Alert: %s %s", application.getJobName(), appState.name());
      sendEmail(subject, html, "****@domain.com");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private AlertTemplate getAlertBaseInfo(Application application) {
    long duration;
    if (application.getEndTime() == null) {
      duration = System.currentTimeMillis() - application.getStartTime().getTime();
    } else {
      duration = application.getEndTime().getTime() - application.getStartTime().getTime();
    }
    String format = "%s/proxy/%s/";
    String url = String.format(format, YarnUtils.getRMWebAppURL(false), application.getAppId());

    AlertTemplate template = new AlertTemplate();
    template.setJobName(application.getJobName());
    template.setStartTime(
        DateUtils.format(
            application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
    template.setDuration(DateUtils.toStringDuration(duration));
    template.setLink(url);
    template.setEndTime(
        DateUtils.format(
            application.getEndTime() == null ? new Date() : application.getEndTime(),
            DateUtils.fullFormat(),
            TimeZone.getDefault()));
    template.setRestart(application.isNeedRestartOnFailed());
    template.setRestartIndex(application.getRestartCount());
    template.setTotalRestart(application.getRestartSize());
    template.setCpFailureRateInterval(
        DateUtils.toStringDuration(application.getCpFailureRateInterval() * 1000 * 60));
    template.setCpMaxFailureInterval(application.getCpMaxFailureInterval());

    return template;
  }

  private void sendEmail(String subject, String html, String... mails) throws EmailException {
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
  }
}
