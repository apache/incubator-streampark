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
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.service.alert.AlertConfigService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Tag(name = "ALERT_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("/flink/alert")
public class AlertController {

  @Autowired private AlertConfigService alertConfigService;

  @Autowired private AlertService alertService;

  @Operation(summary = "Create alert config")
  @PostMapping(value = "/add")
  public RestResponse createAlertConfig(@RequestBody AlertConfigWithParams params) {
    boolean save = alertConfigService.save(AlertConfig.of(params));
    return RestResponse.success(save);
  }

  @Operation(summary = "Check alert config exist")
  @PostMapping(value = "/exists")
  public RestResponse verifyAlertConfig(@RequestBody AlertConfigWithParams params) {
    boolean exist = alertConfigService.exist(AlertConfig.of(params));
    return RestResponse.success(exist);
  }

  @Operation(summary = "Update alert config")
  @PostMapping(value = "/update")
  public RestResponse updateAlertConfig(@RequestBody AlertConfigWithParams params) {
    boolean update = alertConfigService.updateById(AlertConfig.of(params));
    return RestResponse.success(update);
  }

  @Operation(summary = "Get alert config")
  @PostMapping("/get")
  public RestResponse getAlertConfig(@RequestBody AlertConfigWithParams params) {
    AlertConfig alertConfig = alertConfigService.getById(params.getId());
    return RestResponse.success(AlertConfigWithParams.of(alertConfig));
  }

  @Operation(summary = "List alert configs (Pagination)")
  @PostMapping(value = "/list")
  public RestResponse alertConfigsPaginationList(
      @RequestBody AlertConfigWithParams params, RestRequest request) {
    IPage<AlertConfigWithParams> page = alertConfigService.page(params, request);
    return RestResponse.success(page);
  }

  @Operation(summary = "List alert configs")
  @PostMapping(value = "/listWithOutPage")
  public RestResponse alertConfigsList() {
    List<AlertConfig> page = alertConfigService.list();
    return RestResponse.success(page);
  }

  @Operation(summary = "Delete alert config")
  @Parameters({@Parameter(name = "id", required = true, description = "config id")})
  @DeleteMapping("/delete")
  public RestResponse deleteAlertConfig(
      @RequestParam("id") @NotNull(message = "config id must be not null") Long id) {
    boolean result = alertConfigService.deleteById(id);
    return RestResponse.success(result);
  }

  @Operation(summary = "Send alert config")
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
    boolean alert =
        alertService.alert(AlertConfigWithParams.of(alertConfigService.getById(id)), alertTemplate);
    return RestResponse.success(alert);
  }
}
