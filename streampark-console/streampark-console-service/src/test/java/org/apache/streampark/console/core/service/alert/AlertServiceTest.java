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
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.bean.AlertDingTalkParams;
import org.apache.streampark.console.core.bean.AlertLarkParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.bean.AlertWeComParams;
import org.apache.streampark.console.core.service.alert.impl.DingTalkAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.LarkAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.WeComAlertNotifyServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.TimeZone;

class AlertServiceTest {
    AlertTemplate alertTemplate;
    AlertConfigWithParams params = new AlertConfigWithParams();
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void before1() {
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
        alertTemplate.setStartTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setDuration("");
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
        alertTemplate.setStartTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setDuration("");
    }

    @Test
    void dingTalkAlertTest() throws Exception {
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
    void weComAlertTest() throws Exception {
        WeComAlertNotifyServiceImpl notifyService = new WeComAlertNotifyServiceImpl(restTemplate);
        notifyService.loadTemplateFile();

        AlertWeComParams weComParams = new AlertWeComParams();
        weComParams.setToken("your_token");

        params.setAlertType(4);
        params.setWeComParams(weComParams);

        notifyService.doAlert(params, alertTemplate);
    }

    @Test
    void larkAlertTest() {
        LarkAlertNotifyServiceImpl notifyService = new LarkAlertNotifyServiceImpl(restTemplate, mapper);
        notifyService.loadTemplateFile();

        AlertLarkParams alertLarkParams = new AlertLarkParams();
        alertLarkParams.setToken("your_token");

        params.setAlertType(16);
        params.setLarkParams(alertLarkParams);

        notifyService.doAlert(params, alertTemplate);
    }
}
