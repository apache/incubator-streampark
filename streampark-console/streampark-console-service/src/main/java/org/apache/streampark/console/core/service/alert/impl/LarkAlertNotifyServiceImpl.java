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
import org.apache.streampark.console.core.bean.AlertLarkParams;
import org.apache.streampark.console.core.bean.AlertLarkRobotResponse;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.service.alert.AlertNotifyService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Lazy
public class LarkAlertNotifyServiceImpl implements AlertNotifyService {

    private final Template template = FreemarkerUtils.loadTemplateFile("alert-lark.ftl");
    private final RestTemplate alertRestTemplate;
    private final ObjectMapper mapper;

    @Value("${streampark.proxy.lark-url:https://open.feishu.cn}")
    private String larkProxyUrl;

    public LarkAlertNotifyServiceImpl(RestTemplate alertRestTemplate, ObjectMapper mapper) {
        this.alertRestTemplate = alertRestTemplate;
        this.mapper = mapper;
    }

    @Override
    public boolean doAlert(AlertConfigParams alertConfig, AlertTemplate alertTemplate) throws AlertException {
        AlertLarkParams alertLarkParams = alertConfig.getLarkParams();
        if (alertLarkParams.getIsAtAll()) {
            alertTemplate.setAtAll(true);
        }
        try {
            // format markdown
            String markdown = FreemarkerUtils.format(template, alertTemplate);
            Map<String, Object> cardMap = mapper.readValue(markdown, new TypeReference<Map<String, Object>>() {
            });
            Map<String, Object> body = renderBody(alertLarkParams, cardMap);
            sendMessage(alertLarkParams, body);
            return true;
        } catch (AlertException alertException) {
            throw alertException;
        } catch (Exception e) {
            throw new AlertException("Failed send lark alert", e);
        }
    }

    @Nonnull
    private Map<String, Object> renderBody(
                                           AlertLarkParams alertLarkParams, Map<String, Object> cardMap) {
        Map<String, Object> body = new HashMap<>();
        // get sign
        if (alertLarkParams.getSecretEnable()) {
            long timestamp = System.currentTimeMillis() / 1000;
            String sign = getSign(alertLarkParams.getSecretToken(), timestamp);
            body.put("timestamp", timestamp);
            body.put("sign", sign);
        }
        body.put("msg_type", "interactive");
        body.put("card", cardMap);
        return body;
    }

    private void sendMessage(AlertLarkParams params, Map<String, Object> body) throws AlertException {
        // get webhook url
        String url = getWebhook(params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        AlertLarkRobotResponse robotResponse;
        try {
            robotResponse = alertRestTemplate.postForObject(url, entity, AlertLarkRobotResponse.class);
        } catch (Exception e) {
            log.error("Failed to request Lark robot alarm,\nurl:{}", url, e);
            throw new AlertException(
                String.format("Failed to request Lark robot alert,%nurl:%s", url), e);
        }

        if (robotResponse == null) {
            throw new AlertException(String.format("Failed to request Lark robot alert,%nurl:%s", url));
        }
        if (robotResponse.getStatusCode() == null || robotResponse.getStatusCode() != 0) {
            throw new AlertException(
                String.format(
                    "Failed to request Lark robot alert,%nurl:%s,%nerrorCode:%d,%nerrorMsg:%s",
                    url, robotResponse.getCode(), robotResponse.getMsg()));
        }
    }

    /**
     * Gets webhook.
     *
     * @param params {@link AlertLarkParams}
     * @return the webhook
     */
    private String getWebhook(AlertLarkParams params) {
        larkProxyUrl = larkProxyUrl.replaceFirst("/open-apis/bot/v2/hook/(.*)", "");
        String url = String.format(larkProxyUrl + "/open-apis/bot/v2/hook/%s", params.getToken());
        if (log.isDebugEnabled()) {
            log.debug("The alarm robot url of Lark is {}", url);
        }
        return url;
    }

    /**
     * Calculate the signature
     *
     * <p>Reference documentation <a
     * href="https://open.feishu.cn/document/ukTMukTMukTM/ucTM5YjL3ETO24yNxkjN#348211be">Customize
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
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            String sign = new String(Base64.getEncoder().encode(signData));
            if (log.isDebugEnabled()) {
                log.debug("Calculate the signature success, sign:{}", sign);
            }
            return sign;
        } catch (Exception e) {
            throw new AlertException("Calculate the signature failed.", e);
        }
    }
}
