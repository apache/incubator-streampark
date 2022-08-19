/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;
import com.streamxhub.streamx.console.core.entity.alert.AlertTemplate;
import com.streamxhub.streamx.console.core.entity.alert.DingTalkParams;
import com.streamxhub.streamx.console.core.entity.alert.LarkParams;
import com.streamxhub.streamx.console.core.entity.alert.WeComParams;
import com.streamxhub.streamx.console.core.service.alert.impl.DingTalkAlertNotifyServiceImpl;
import com.streamxhub.streamx.console.core.service.alert.impl.LarkAlertNotifyServiceImpl;
import com.streamxhub.streamx.console.core.service.alert.impl.WeComAlertNotifyServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.TimeZone;

/**
 * @author weijinglun
 * @date 2022.01.19
 */
public class AlertServiceTest {
    AlertTemplate alertTemplate;
    AlertConfigWithParams params = new AlertConfigWithParams();
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();

    @Before
    public void before1() {
        alertTemplate = new AlertTemplate();
        alertTemplate.setTitle("Notify: StreamX alert job for test");
        alertTemplate.setSubject("StreamX Alert: test-job OTHER");
        alertTemplate.setJobName("StreamX alert job for test");
        alertTemplate.setLink("http://127.0.0.1:8080");
        alertTemplate.setStatus("TEST");
        alertTemplate.setType(1);
        alertTemplate.setRestart(true);
        alertTemplate.setTotalRestart(5);
        alertTemplate.setRestartIndex(2);
        Date date = new Date();
        alertTemplate.setStartTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setDuration(DateUtils.toRichTimeDuration(0));
    }

    public void before2() {
        alertTemplate = new AlertTemplate();
        alertTemplate.setTitle("告警: StreamX alert job for test");
        alertTemplate.setSubject("StreamX Alert: test-job OTHER");
        alertTemplate.setJobName("StreamX alert job for test");
        alertTemplate.setLink("http://127.0.0.1:8080");
        alertTemplate.setStatus("TEST");
        alertTemplate.setType(2);
        alertTemplate.setCpMaxFailureInterval(5);
        alertTemplate.setCpFailureRateInterval("10%");
        Date date = new Date();
        alertTemplate.setStartTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setDuration(DateUtils.toRichTimeDuration(0));
    }

    @Test
    public void dingTalkAlertTest() throws Exception {
        DingTalkAlertNotifyServiceImpl notifyService = new DingTalkAlertNotifyServiceImpl(restTemplate);

        notifyService.loadTemplateFile();
        DingTalkParams dingTalkParams = new DingTalkParams();
        dingTalkParams.setToken("your_token");
        dingTalkParams.setContacts("175xxxx1234");
        dingTalkParams.setIsAtAll(true);

        params.setAlertType(2);
        params.setDingTalkParams(dingTalkParams);

        notifyService.doAlert(params, alertTemplate);
    }

    @Test
    public void weComAlertTest() throws Exception {
        WeComAlertNotifyServiceImpl notifyService = new WeComAlertNotifyServiceImpl(restTemplate);
        notifyService.loadTemplateFile();

        WeComParams weComParams = new WeComParams();
        weComParams.setToken("your_token");

        params.setAlertType(4);
        params.setWeComParams(weComParams);

        notifyService.doAlert(params, alertTemplate);
    }

    @Test
    public void larkAlertTest() throws Exception {
        LarkAlertNotifyServiceImpl notifyService = new LarkAlertNotifyServiceImpl(restTemplate, mapper);
        notifyService.loadTemplateFile();

        LarkParams larkParams = new LarkParams();
        larkParams.setToken("your_token");

        params.setAlertType(16);
        params.setLarkParams(larkParams);

        notifyService.doAlert(params, alertTemplate);
    }
}
