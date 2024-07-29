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
import org.apache.streampark.console.core.bean.AlertWeComParams;
import org.apache.streampark.console.core.bean.RobotResponse;
import org.apache.streampark.console.core.service.alert.AlertNotifyService;

import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Lazy
public class WeComAlertNotifyServiceImpl implements AlertNotifyService {

    private final Template template = FreemarkerUtils.loadTemplateFile("alert-weCom.ftl");

    private final RestTemplate alertRestTemplate;

    public WeComAlertNotifyServiceImpl(RestTemplate alertRestTemplate) {
        this.alertRestTemplate = alertRestTemplate;
    }

    @Override
    public boolean doAlert(AlertConfigParams alertConfig, AlertTemplate alertTemplate) throws AlertException {
        AlertWeComParams weComParams = alertConfig.getWeComParams();
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

    private void sendMessage(AlertWeComParams params, Map<String, Object> body) throws AlertException {
        // get webhook url
        String url = getWebhook(params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        RobotResponse robotResponse;
        try {
            robotResponse = alertRestTemplate.postForObject(url, entity, RobotResponse.class);
        } catch (Exception e) {
            log.error("Failed to request WeCom robot alarm,%nurl:{}", url, e);
            throw new AlertException(
                String.format("Failed to request WeCom robot alert,%nurl:%s", url), e);
        }

        if (robotResponse == null) {
            throw new AlertException(String.format("Failed to request WeCom robot alert,%nurl:%s", url));
        }
        if (robotResponse.getErrcode() != 0) {
            throw new AlertException(
                String.format(
                    "Failed to request WeCom robot alert,%nurl:%s,%nerrorCode:%d,%nerrorMsg:%s",
                    url, robotResponse.getErrcode(), robotResponse.getErrmsg()));
        }
    }

    /**
     * Gets webhook.
     *
     * <p>Reference documentation <a
     * href="https://developer.work.weixin.qq.com/document/path/91770">Swarm Robot Configuration
     * Instructions</a>
     *
     * @param params {@link AlertWeComParams}
     * @return the webhook
     */
    private String getWebhook(AlertWeComParams params) {
        String url;
        url = String.format("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=%s", params.getToken());
        if (log.isDebugEnabled()) {
            log.debug("The alert robot url of WeCom is {}", url);
        }
        return url;
    }
}
