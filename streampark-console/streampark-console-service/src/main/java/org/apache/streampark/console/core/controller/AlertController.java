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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.service.alert.AlertConfigService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/flink/alert")
public class AlertController {

    private final AlertConfigService alertConfigService;

    private final AlertService alertService;

    @PostMapping("/add")
    public RestResponse createAlertConfig(@RequestBody AlertConfigParams params) {
        boolean save = alertConfigService.save(AlertConfig.of(params));
        return RestResponse.success(save);
    }

    @PostMapping("/exists")
    public RestResponse verifyAlertConfig(@RequestBody AlertConfigParams params) {
        boolean exist = alertConfigService.exist(AlertConfig.of(params));
        return RestResponse.success(exist);
    }

    @PostMapping("/update")
    public RestResponse updateAlertConfig(@RequestBody AlertConfigParams params) {
        boolean update = alertConfigService.updateById(AlertConfig.of(params));
        return RestResponse.success(update);
    }

    @PostMapping("/get")
    public RestResponse getAlertConfig(@RequestBody AlertConfigParams params) {
        AlertConfig alertConfig = alertConfigService.getById(params.getId());
        return RestResponse.success(AlertConfigParams.of(alertConfig));
    }

    @PostMapping("/page")
    public RestResponse pageAlertConfig(
                                        @RequestBody AlertConfigParams params, RestRequest request) {
        IPage<AlertConfigParams> page = alertConfigService.page(params.getUserId(), request);
        return RestResponse.success(page);
    }

    @PostMapping("/list")
    public RestResponse listAlertConfig() {
        List<AlertConfig> page = alertConfigService.list();
        return RestResponse.success(page);
    }

    @DeleteMapping("/delete")
    public RestResponse deleteAlertConfig(@NotNull(message = "{required}") Long id) {
        boolean result = alertConfigService.removeById(id);
        return RestResponse.success(result);
    }

    @PostMapping("/send")
    public RestResponse sendAlert(Long id) throws AlertException {
        AlertTemplate alertTemplate = new AlertTemplate();
        alertTemplate.setTitle("Notify: StreamPark alert job for test");
        alertTemplate.setJobName("StreamPark alert job for test");
        alertTemplate.setSubject("StreamPark Alert: Test");
        alertTemplate.setStatus("TEST");
        alertTemplate.setType(1);
        alertTemplate.setRestart(false);
        Date date = new Date();
        alertTemplate.setStartTime(
            DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setDuration("");
        return RestResponse.success(alertService.alert(id, alertTemplate));
    }
}
