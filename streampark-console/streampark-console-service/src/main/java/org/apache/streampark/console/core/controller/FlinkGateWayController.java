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

import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.core.entity.FlinkGateWay;
import org.apache.streampark.console.core.enums.GatewayTypeEnum;
import org.apache.streampark.console.core.service.FlinkGateWayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("flink/gateway")
public class FlinkGateWayController {

    private final FlinkGateWayService flinkGatewayService;

    @GetMapping("list")
    public Result<List<FlinkGateWay>> list() {
        return Result.success(flinkGatewayService.list());
    }

    @PostMapping("create")
    public Result<Void> create(@RequestBody FlinkGateWay flinkGateWay) {
        flinkGatewayService.create(flinkGateWay);
        return Result.success();
    }

    @GetMapping("check/name")
    public Result<Boolean> checkName(
                                     @NotNull(message = "The Gateway name cannot be null") @RequestParam("name") String name) {
        return Result.success(flinkGatewayService.existsByGatewayName(name));
    }

    @GetMapping("check/address")
    public Result<GatewayTypeEnum> checkAddress(
                                                @NotNull(message = "The Gateway address cannot be null") @RequestParam("address") String address) throws Exception {
        GatewayTypeEnum gatewayVersion = flinkGatewayService.getGatewayVersion(address);
        return Result.success(gatewayVersion);
    }

    @PutMapping("update")
    public Result<Void> update(@RequestBody FlinkGateWay flinkGateWay) {
        flinkGatewayService.update(flinkGateWay);
        return Result.success();
    }

    @GetMapping("get/{id}")
    public Result<FlinkGateWay> get(@PathVariable Long id) {
        return Result.success(flinkGatewayService.getById(id));
    }

    @DeleteMapping("delete")
    public Result<Void> delete(
                               @NotNull(message = "The Gateway id cannot be null") @RequestParam("id") Long id) {
        flinkGatewayService.removeById(id);
        return Result.success();
    }
}
