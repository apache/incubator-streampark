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
import com.streamxhub.streamx.console.core.entity.alert.RobotResponse;
import com.streamxhub.streamx.console.core.entity.alert.WeComParams;
import com.streamxhub.streamx.console.core.service.alert.AlertNotifyService;

import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
public class WeComAlertNotifyServiceImpl implements AlertNotifyService {
    private Template template;

    private final RestTemplate alertRestTemplate;

    public WeComAlertNotifyServiceImpl(RestTemplate alertRestTemplate) {
        this.alertRestTemplate = alertRestTemplate;
    }

    @PostConstruct
    public void loadTemplateFile() throws Exception {
        String template = "alert-weCom.ftl";
        this.template = FreemarkerUtils.loadTemplateFile(template);
    }

    @Override
    public boolean doAlert(AlertConfigWithParams alertConfig, AlertTemplate alertTemplate) throws AlertException {
        WeComParams weComParams = alertConfig.getWeComParams();
        try {
            // format markdown
            String markdown = FreemarkerUtils.format(template, alertTemplate);

            Map<String, String> content = new HashMap<>();
            content.put("content", markdown);

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");
            body.put("markdown", content);

            sendMessage(weComParams, body);
            return true;
        } catch (AlertException alertException) {
            throw alertException;
        } catch (Exception e) {
            throw new AlertException("Failed send weCom alert", e);
        }
    }

    private void sendMessage(WeComParams params, Map<String, Object> body) throws AlertException {
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
            throw new AlertException(String.format("Failed to request WeCom robot alert,\nurl:%s", url), e);
        }

        if (robotResponse == null) {
            throw new AlertException(String.format("Failed to request WeCom robot alert,\nurl:%s", url));
        }
        if (robotResponse.getErrcode() != 0) {
            throw new AlertException(String.format("Failed to request DingTalk robot alert,\nurl:%s,\nerrorCode:%d,\nerrorMsg:%s",
                    url, robotResponse.getErrcode(), robotResponse.getErrmsg()));
        }
    }

    /**
     * Gets webhook.
     * <p>Reference documentation</p>
     * <a href="https://developer.work.weixin.qq.com/document/path/91770">Swarm Robot Configuration Instructions</a>
     *
     * @param params {@link  WeComParams}
     * @return the webhook
     */
    private String getWebhook(WeComParams params) {
        String url;
        url = String.format("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=%s", params.getToken());
        if (log.isDebugEnabled()) {
            log.debug("The alert robot url of WeCom is {}", url);
        }
        return url;
    }

}
