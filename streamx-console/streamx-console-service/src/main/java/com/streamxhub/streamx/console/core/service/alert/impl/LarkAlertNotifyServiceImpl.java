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
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;
import com.streamxhub.streamx.console.core.entity.alert.AlertTemplate;
import com.streamxhub.streamx.console.core.entity.alert.LarkParams;
import com.streamxhub.streamx.console.core.entity.alert.LarkRobotResponse;
import com.streamxhub.streamx.console.core.service.alert.AlertNotifyService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.util.Base64;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author weijinglun
 * @date 2022.06.24
 */
@Slf4j
@Service
@Lazy
public class LarkAlertNotifyServiceImpl implements AlertNotifyService {
    private Template template;
    private final RestTemplate alertRestTemplate;
    private final ObjectMapper mapper;

    public LarkAlertNotifyServiceImpl(RestTemplate alertRestTemplate, ObjectMapper mapper) {
        this.alertRestTemplate = alertRestTemplate;
        this.mapper = mapper;
    }

    @PostConstruct
    public void loadTemplateFile() {
        String template = "alert-lark.ftl";
        this.template = FreemarkerUtils.loadTemplateFile(template);
    }

    @Override
    public boolean doAlert(AlertConfigWithParams alertConfig, AlertTemplate alertTemplate) throws AlertException {
        LarkParams larkParams = alertConfig.getLarkParams();
        if (larkParams.getIsAtAll()) {
            alertTemplate.setAtAll(true);
        }
        try {
            // format markdown
            String markdown = FreemarkerUtils.format(template, alertTemplate);
            Map<String, Object> cardMap = mapper.readValue(markdown, new TypeReference<Map<String, Object>>() {
            });

            Map<String, Object> body = new HashMap<>();
            // get sign
            if (larkParams.getSecretEnable()) {
                long timestamp = System.currentTimeMillis() / 1000;
                String sign = getSign(larkParams.getSecretToken(), timestamp);
                body.put("timestamp", timestamp);
                body.put("sign", sign);
            }
            body.put("msg_type", "interactive");
            body.put("card", cardMap);
            sendMessage(larkParams, body);
            return true;
        } catch (AlertException alertException) {
            throw alertException;
        } catch (Exception e) {
            throw new AlertException("Failed send lark alert", e);
        }
    }

    private void sendMessage(LarkParams params, Map<String, Object> body) throws AlertException {
        // get webhook url
        String url = getWebhook(params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        LarkRobotResponse robotResponse;
        try {
            robotResponse = alertRestTemplate.postForObject(url, entity, LarkRobotResponse.class);
        } catch (Exception e) {
            log.error("Failed to request Lark robot alarm,\nurl:{}", url, e);
            throw new AlertException(String.format("Failed to request Lark robot alert,\nurl:%s", url), e);
        }

        if (robotResponse == null) {
            throw new AlertException(String.format("Failed to request Lark robot alert,\nurl:%s", url));
        }
        if (robotResponse.getStatusCode() == null || robotResponse.getStatusCode() != 0) {
            throw new AlertException(String.format("Failed to request Lark robot alert,\nurl:%s,\nerrorCode:%d,\nerrorMsg:%s",
                    url, robotResponse.getCode(), robotResponse.getMsg()));
        }
    }

    /**
     * Gets webhook.
     *
     * @param params {@link  LarkParams}
     * @return the webhook
     */
    private String getWebhook(LarkParams params) {
        String url = String.format("https://open.feishu.cn/open-apis/bot/v2/hook/%s", params.getToken());
        if (log.isDebugEnabled()) {
            log.debug("The alarm robot url of Lark is {}", url);
        }
        return url;
    }

    /**
     * Calculate the signature
     * <p>Reference documentation</p>
     * <a href="https://open.feishu.cn/document/ukTMukTMukTM/ucTM5YjL3ETO24yNxkjN#348211be">Customize Robot Security Settings</a>
     *
     * @param secret    secret
     * @param timestamp current timestamp
     * @return Signature information calculated from timestamp
     */
    private String getSign(String secret, Long timestamp) throws AlertException {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            String sign = new String(Base64.encodeBase64(signData));
            if (log.isDebugEnabled()) {
                log.debug("Calculate the signature success, sign:{}", sign);
            }
            return sign;
        } catch (Exception e) {
            throw new AlertException("Calculate the signature failed.", e);
        }
    }
}
