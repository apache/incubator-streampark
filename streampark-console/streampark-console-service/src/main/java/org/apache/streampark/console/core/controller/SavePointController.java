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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;

@Tag(name = "SAVEPOINT_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/savepoint")
public class SavePointController {

  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private SavePointService savePointService;

  @Operation(summary = "Get application savepoint latest")
  @PostMapping("latest")
  public RestResponse latest(Long appId) {
    SavePoint savePoint = savePointService.getLatest(appId);
    return RestResponse.success(savePoint);
  }

  @Operation(summary = "List application savepoint histories")
  @PostMapping("history")
  public RestResponse history(SavePoint savePoint, RestRequest request) {
    IPage<SavePoint> page = savePointService.getPage(savePoint, request);
    return RestResponse.success(page);
  }

  @Operation(summary = "Delete savepoint")
  @PostMapping("delete")
  @RequiresPermissions("savepoint:delete")
  public RestResponse delete(Long id) throws InternalException {
    SavePoint savePoint = savePointService.getById(id);
    Application application = applicationManageService.getById(savePoint.getAppId());
    Boolean deleted = savePointService.remove(id, application);
    return RestResponse.success(deleted);
  }

  @Operation(
      summary = "Trigger savepoint",
      description = "trigger savepoint for specified application")
  @Parameters({
    @Parameter(
        name = "appId",
        description = "app id",
        required = true,
        example = "100000",
        schema = @Schema(implementation = Long.class)),
    @Parameter(
        name = "savepointPath",
        description = "specified savepoint path",
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "nativeFormat",
        description = "use native format",
        schema = @Schema(implementation = Boolean.class))
  })
  @ApiAccess
  @PostMapping("trigger")
  @RequiresPermissions("savepoint:trigger")
  public RestResponse trigger(
      Long appId, @Nullable String savepointPath, @Nullable Boolean nativeFormat) {
    savePointService.trigger(appId, savepointPath, nativeFormat);
    return RestResponse.success(true);
  }
}
