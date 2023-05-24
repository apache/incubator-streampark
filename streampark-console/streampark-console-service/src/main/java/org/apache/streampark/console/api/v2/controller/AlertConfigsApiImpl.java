/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.streampark.console.api.v2.controller;

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.api.controller.AlertConfigsApi;
import org.apache.streampark.console.api.controller.model.AlertConfigInfo;
import org.apache.streampark.console.api.controller.model.CreateAlertConfigRequest;
import org.apache.streampark.console.api.controller.model.ListAlertConfig;
import org.apache.streampark.console.api.controller.model.UpdateAlertConfigRequest;
import org.apache.streampark.console.api.v2.convert.AlertConfigConvert;
import org.apache.streampark.console.api.v2.exception.NotFoundException;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.service.alert.AlertConfigService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@RestController
public class AlertConfigsApiImpl implements AlertConfigsApi {

  @Resource AlertConfigConvert convert;

  @Autowired AlertConfigService alertConfigService;

  @Autowired AlertService alertService;

  @Override
  public ResponseEntity<AlertConfigInfo> createAlertConfig(CreateAlertConfigRequest request) {
    AlertConfig config = convert.toPo(request);
    alertConfigService.save(config);
    return ResponseEntity.ok(convert.toVo(config));
  }

  @Override
  public ResponseEntity<Void> deleteAlertConfig(Long configId) {
    Optional.ofNullable(alertConfigService.getById(configId))
        .orElseThrow(() -> new NotFoundException("ResourceNotFound"));
    alertConfigService.deleteById(configId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<AlertConfigInfo> getAlertConfig(Long configId) {
    AlertConfig config =
        Optional.ofNullable(alertConfigService.getById(configId))
            .orElseThrow(() -> new NotFoundException("ResourceNotFound"));
    return ResponseEntity.ok(convert.toVo(config));
  }

  @Override
  public ResponseEntity<ListAlertConfig> listAlertConfig(
      Long userId, Integer offset, Integer limit) {
    List<AlertConfig> configs;
    long size;
    if (limit != null) {
      IPage<AlertConfig> page = alertConfigService.page(userId, new RestRequest(offset, limit));
      configs = page.getRecords();
      size = page.getTotal();
    } else {
      configs = alertConfigService.list();
      size = configs.size();
    }
    ListAlertConfig resp =
        ListAlertConfig.builder().alertConfigs(convert.toVo(configs)).total(size).build();
    return ResponseEntity.ok(resp);
  }

  @Override
  public ResponseEntity<AlertConfigInfo> updateAlertConfig(
      Long configId, UpdateAlertConfigRequest updateAlertConfigRequest) {
    Optional.ofNullable(alertConfigService.getById(configId))
        .orElseThrow(() -> new NotFoundException("ResourceNotFound"));
    AlertConfig po = convert.toPo(updateAlertConfigRequest);
    po.setId(configId);
    alertConfigService.updateById(po);
    return ResponseEntity.ok(convert.toVo(po));
  }

  @Override
  public ResponseEntity<Void> testSendAlertConfig(Long configId) {
    AlertConfig config =
        Optional.ofNullable(alertConfigService.getById(configId))
            .orElseThrow(() -> new NotFoundException("ResourceNotFound"));
    AlertTemplate alertTemplate =
        new AlertTemplate()
            .setTitle("Notify: StreamPark alert job for test")
            .setJobName("StreamPark alert job for test")
            .setSubject("StreamPark Alert: Test")
            .setStatus("TEST")
            .setType(1)
            .setRestart(false)
            .setStartTime(
                DateUtils.format(new Date(), DateUtils.fullFormat(), TimeZone.getDefault()))
            .setEndTime(DateUtils.format(new Date(), DateUtils.fullFormat(), TimeZone.getDefault()))
            .setDuration("");
    alertService.alert(AlertConfigWithParams.of(config), alertTemplate);
    return ResponseEntity.noContent().build();
  }
}
