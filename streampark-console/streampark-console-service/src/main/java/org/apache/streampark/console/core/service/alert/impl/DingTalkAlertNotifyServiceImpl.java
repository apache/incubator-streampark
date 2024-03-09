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

import org.apache.streampark.common.exception.AlertException;
import org.apache.streampark.common.util.PremisesUtils;
import org.apache.streampark.console.base.util.FreemarkerUtils;
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.bean.AlertDingTalkParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.bean.RobotResponse;
import org.apache.streampark.console.core.service.alert.AlertNotifyService;

import org.apache.commons.lang3.BooleanUtils;

import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Service
@Lazy
public class DingTalkAlertNotifyServiceImpl implements AlertNotifyService {
  private final Template template = FreemarkerUtils.loadTemplateFile("alert-dingTalk.ftl");

  private final RestTemplate alertRestTemplate;

  public DingTalkAlertNotifyServiceImpl(RestTemplate alertRestTemplate) {
    this.alertRestTemplate = alertRestTemplate;
  }

  @Override
  public boolean doAlert(AlertConfigParams alertConfig, AlertTemplate alertTemplate)
      throws AlertException {
    AlertDingTalkParams dingTalkParams = alertConfig.getDingTalkParams();
    try {
      // handling contacts
      List<String> contactList = new ArrayList<>();
      String contacts = dingTalkParams.getContacts();
      if (StringUtils.hasLength(contacts)) {
        Collections.addAll(contactList, contacts.split(","));
      }
      String title = renderTitle(alertTemplate, contactList);
      Map<String, Object> contactMap = renderContact(contactList, dingTalkParams);
      // format markdown
      String markdown = FreemarkerUtils.format(template, alertTemplate);
      Map<String, String> contentMap = renderContent(title, markdown);
      Map<String, Object> bodyMap = renderBody(contentMap, contactMap);

      sendMessage(dingTalkParams, bodyMap);
      return true;
    } catch (AlertException alertException) {
      throw alertException;
    } catch (Exception e) {
      throw new AlertException("Failed send DingTalk alert", e);
    }
  }

  @Nonnull
  private Map<String, Object> renderBody(
      Map<String, String> content, Map<String, Object> contactMap) {
    Map<String, Object> body = new HashMap<>();
    body.put("msgtype", "markdown");
    body.put("markdown", content);
    body.put("at", contactMap);
    return body;
  }

  @Nonnull
  private Map<String, String> renderContent(String title, String markdown) {
    Map<String, String> content = new HashMap<>();
    content.put("title", title);
    content.put("text", markdown);
    return content;
  }

  @Nonnull
  private Map<String, Object> renderContact(
      List<String> contactList, AlertDingTalkParams dingTalkParams) {
    Map<String, Object> contactMap = new HashMap<>();
    contactMap.put("atMobiles", contactList);
    contactMap.put("isAtAll", BooleanUtils.toBoolean(dingTalkParams.getIsAtAll()));
    return contactMap;
  }

  private String renderTitle(AlertTemplate alertTemplate, List<String> contactList) {
    String title = alertTemplate.getTitle();
    if (!contactList.isEmpty()) {
      StringJoiner joiner = new StringJoiner(",@", title + " @", "");
      contactList.forEach(joiner::add);
      title = joiner.toString();
    }
    return title;
  }

  private void sendMessage(AlertDingTalkParams params, Map<String, Object> body)
      throws AlertException {
    // get webhook url
    String url = getWebhook(params);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    RobotResponse robotResponse;
    try {
      robotResponse = alertRestTemplate.postForObject(url, entity, RobotResponse.class);
    } catch (Exception e) {
      log.error("Failed to request DingTalk robot alarm,\nurl:{}", url, e);
      throw new AlertException(
          String.format("Failed to request DingTalk robot alert,%nurl:%s", url), e);
    }

    PremisesUtils.throwIfNull(
        robotResponse,
        String.format("Failed to request DingTalk robot alert,%nurl:%s", url),
        AlertException.class);

    PremisesUtils.throwIfTrue(
        robotResponse.getErrcode() != 0,
        String.format(
            "Failed to request DingTalk robot alert,%nurl:%s,%nerrorCode:%d,%nerrorMsg:%s",
            url, robotResponse.getErrcode(), robotResponse.getErrmsg()),
        AlertException.class);
  }

  /**
   * Gets webhook.
   *
   * @param params {@link AlertDingTalkParams}
   * @return the webhook
   */
  private String getWebhook(AlertDingTalkParams params) {
    String urlPef = "https://oapi.dingtalk.com/robot/send";
    if (StringUtils.hasLength(params.getAlertDingURL())) {
      urlPef = params.getAlertDingURL();
    }
    if (!urlPef.endsWith("access_token=")) {
      urlPef += "?access_token=";
    }

    String url;
    if (params.getSecretEnable()) {
      Long timestamp = System.currentTimeMillis();
      url =
          String.format(
              "%s%s&timestamp=%d&sign=%s",
              urlPef, params.getToken(), timestamp, getSign(params.getSecretToken(), timestamp));
    } else {
      url = String.format("%s%s", urlPef, params.getToken());
    }
    if (log.isDebugEnabled()) {
      log.debug("The alarm robot url of DingTalk contains signature is {}", url);
    }
    return url;
  }

  /**
   * Calculate the signature
   *
   * <p>Reference documentation <a
   * href="https://open.dingtalk.com/document/group/customize-robot-security-settings">Customize
   * Robot Security Settings</a>
   *
   * @param secret secret
   * @param timestamp current timestamp
   * @return Signature information calculated from timestamp
   */
  private String getSign(String secret, Long timestamp) throws AlertException {
    try {
      String stringToSign = timestamp + "\n" + secret;
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
      String sign = URLEncoder.encode(new String(Base64.getEncoder().encode(signData)), "UTF-8");
      if (log.isDebugEnabled()) {
        log.debug("Calculate the signature success, sign:{}", sign);
      }
      return sign;
    } catch (Exception e) {
      throw new AlertException("Calculate the signature failed.", e);
    }
  }
}
