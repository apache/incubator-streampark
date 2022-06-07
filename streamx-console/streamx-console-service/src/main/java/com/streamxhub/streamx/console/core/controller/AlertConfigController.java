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

package com.streamxhub.streamx.console.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfig;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;
import com.streamxhub.streamx.console.core.entity.alert.AlertTemplate;
import com.streamxhub.streamx.console.core.service.alert.AlertConfigService;
import com.streamxhub.streamx.console.core.service.alert.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
@Slf4j
@Validated
@RestController
@RequestMapping("flink/alert/conf")
public class AlertConfigController {

    @Autowired
    private AlertConfigService alertConfigService;

    @Autowired
    private AlertService alertService;
    @PostMapping(value = "add")
    public RestResponse addAlertConf(AlertConfigWithParams params) throws Exception {
        AlertConfig alertConfig = AlertConfig.of(params);
        boolean save = alertConfigService.save(alertConfig);
        return RestResponse.create().data(save);
    }

    @PostMapping(value = "exists")
    public RestResponse exists(AlertConfigWithParams params) throws Exception {
        AlertConfig alertConfig = AlertConfig.of(params);
        boolean exist = alertConfigService.exist(alertConfig);
        return RestResponse.create().data(exist);
    }

    @PutMapping(value = "update")
    public RestResponse update(AlertConfigWithParams params) throws Exception {
        boolean update = alertConfigService.updateById(AlertConfig.of(params));
        return RestResponse.create().data(update);
    }

    @PostMapping("get")
    public RestResponse get(AlertConfigWithParams params) throws Exception {
        AlertConfig alertConfig = alertConfigService.getById(params.getId());
        return RestResponse.create().data(AlertConfigWithParams.of(alertConfig));
    }

    @PostMapping(value = "list")
    public RestResponse list(AlertConfigWithParams params, RestRequest request) throws Exception {
        IPage<AlertConfigWithParams> page = alertConfigService.page(params, request);
        return RestResponse.create().data(page);
    }

    @DeleteMapping("delete")
    public RestResponse deleteAlertConf(@NotNull(message = "id must not be empty") Long id) {
        boolean result = alertConfigService.removeById(id);
        return RestResponse.create().data(result);
    }

    /**
     * send alert message for test
     */
    @PostMapping("send")
    public RestResponse sendAlert(AlertConfigWithParams params) throws Exception {
        AlertTemplate alertTemplate = new AlertTemplate();
        alertTemplate.setTitle("Notify: StreamX alert job for test");
        alertTemplate.setJobName("StreamX alert job for test");
        alertTemplate.setLink("http://127.0.0.1:8080");
        alertTemplate.setStatus("TEST");
        Date date = new Date();
        alertTemplate.setStartTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setEndTime(DateUtils.format(date, DateUtils.fullFormat(), TimeZone.getDefault()));
        alertTemplate.setDuration(DateUtils.toRichTimeDuration(0));

        boolean alert = alertService.alert(params, alertTemplate);
        return RestResponse.create().data(alert);
    }

}
