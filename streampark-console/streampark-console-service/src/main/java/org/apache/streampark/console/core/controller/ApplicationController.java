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

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.domain.ApiDocConstant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.annotation.AppUpdated;
import org.apache.streampark.console.core.annotation.PermissionAction;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationBackUp;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.enums.PermissionTypeEnum;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

@Tag(name = "FLINK_APPLICATION_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/app")
public class ApplicationController {

  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private ApplicationActionService applicationActionService;

  @Autowired private ApplicationInfoService applicationInfoService;

  @Autowired private ApplicationBackUpService backUpService;

  @Autowired private ApplicationLogService applicationLogService;

  @Autowired private ResourceService resourceService;

  @Operation(summary = "Get application")
  @ApiAccess
  @PostMapping("get")
  @RequiresPermissions("app:detail")
  public RestResponse get(Application app) {
    Application application = applicationManageService.getApp(app);
    return RestResponse.success(application);
  }

  @Operation(summary = "Create application")
  @ApiAccess
  @PermissionAction(id = "#app.teamId", type = PermissionTypeEnum.TEAM)
  @PostMapping("create")
  @RequiresPermissions("app:create")
  public RestResponse create(Application app) throws IOException {
    boolean saved = applicationManageService.create(app);
    return RestResponse.success(saved);
  }

  @Operation(
      summary = "Copy application",
      tags = {ApiDocConstant.FLINK_APP_OP_TAG})
  @Parameters({
    @Parameter(
        name = "id",
        description = "copied target app id",
        in = ParameterIn.QUERY,
        required = true,
        example = "100000"),
    @Parameter(
        name = "jobName",
        description = "new application name",
        in = ParameterIn.QUERY,
        example = "copy-app"),
    @Parameter(name = "args", description = "new application args", in = ParameterIn.QUERY)
  })
  @ApiAccess
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping(value = "copy")
  @RequiresPermissions("app:copy")
  public RestResponse copy(@Parameter(hidden = true) Application app) throws IOException {
    applicationManageService.copy(app);
    return RestResponse.success();
  }

  @Operation(summary = "Update application")
  @AppUpdated
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping("update")
  @RequiresPermissions("app:update")
  public RestResponse update(Application app) {
    applicationManageService.update(app);
    return RestResponse.success(true);
  }

  @Operation(summary = "Get applications dashboard data")
  @PostMapping("dashboard")
  public RestResponse dashboard(Long teamId) {
    Map<String, Serializable> dashboardMap = applicationInfoService.getDashboardDataMap(teamId);
    return RestResponse.success(dashboardMap);
  }

  @Operation(summary = "List applications")
  @ApiAccess
  @PostMapping("list")
  @RequiresPermissions("app:view")
  public RestResponse list(Application app, RestRequest request) {
    IPage<Application> applicationList = applicationManageService.page(app, request);
    return RestResponse.success(applicationList);
  }

  @Operation(summary = "Mapping application")
  @AppUpdated
  @PostMapping("mapping")
  @RequiresPermissions("app:mapping")
  public RestResponse mapping(Application app) {
    boolean flag = applicationManageService.mapping(app);
    return RestResponse.success(flag);
  }

  @Operation(summary = "Revoke application")
  @AppUpdated
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping("revoke")
  @RequiresPermissions("app:release")
  public RestResponse revoke(Application app) {
    applicationActionService.revoke(app);
    return RestResponse.success();
  }

  @Operation(
      summary = "Start application",
      tags = {ApiDocConstant.FLINK_APP_OP_TAG})
  @Parameters({
    @Parameter(
        name = "id",
        description = "start app id",
        in = ParameterIn.QUERY,
        required = true,
        example = "100000",
        schema = @Schema(implementation = Long.class)),
    @Parameter(
        name = "savePointed",
        description = "restored app from the savepoint or latest checkpoint",
        in = ParameterIn.QUERY,
        required = true,
        example = "false",
        schema = @Schema(implementation = boolean.class, defaultValue = "false")),
    @Parameter(
        name = "savePoint",
        description = "savepoint or checkpoint path",
        in = ParameterIn.QUERY,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "allowNonRestored",
        description = "ignore savepoint if cannot be restored",
        in = ParameterIn.QUERY,
        required = false,
        schema = @Schema(implementation = boolean.class, defaultValue = "false"))
  })
  @ApiAccess
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping(value = "start")
  @RequiresPermissions("app:start")
  public RestResponse start(@Parameter(hidden = true) Application app) {
    try {
      applicationActionService.start(app, false);
      return RestResponse.success(true);
    } catch (Exception e) {
      return RestResponse.success(false).message(e.getMessage());
    }
  }

  @Operation(
      summary = "Cancel application",
      tags = {ApiDocConstant.FLINK_APP_OP_TAG})
  @ApiAccess
  @Parameters({
    @Parameter(
        name = "id",
        description = "cancel app id",
        in = ParameterIn.QUERY,
        required = true,
        example = "100000",
        schema = @Schema(implementation = Long.class)),
    @Parameter(
        name = "savePointed",
        description = "trigger savepoint before taking stopping",
        in = ParameterIn.QUERY,
        required = true,
        schema = @Schema(implementation = boolean.class, defaultValue = "false")),
    @Parameter(
        name = "savePoint",
        description = "savepoint path",
        in = ParameterIn.QUERY,
        example = "hdfs:///savepoint/100000",
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "drain",
        description = "send max watermark before canceling",
        in = ParameterIn.QUERY,
        required = true,
        example = "false",
        schema = @Schema(implementation = boolean.class, defaultValue = "false")),
    @Parameter(
        name = "nativeFormat",
        description = "use savepoint native format",
        in = ParameterIn.QUERY,
        required = true,
        example = "false",
        schema = @Schema(implementation = boolean.class, defaultValue = "false"))
  })
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping(value = "cancel")
  @RequiresPermissions("app:cancel")
  public RestResponse cancel(@Parameter(hidden = true) Application app) throws Exception {
    applicationActionService.cancel(app);
    return RestResponse.success();
  }

  @Operation(summary = "Clean application")
  @AppUpdated
  @ApiAccess
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping("clean")
  @RequiresPermissions("app:clean")
  public RestResponse clean(Application app) {
    applicationManageService.clean(app);
    return RestResponse.success(true);
  }

  /** force stop(stop normal start or in progress) */
  @Operation(summary = "Force stop application")
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping("forcedStop")
  @RequiresPermissions("app:cancel")
  public RestResponse forcedStop(Application app) {
    applicationActionService.forcedStop(app);
    return RestResponse.success();
  }

  @Operation(summary = "Get application on yarn proxy address")
  @PostMapping("yarn")
  public RestResponse yarn() {
    return RestResponse.success(YarnUtils.getRMWebAppProxyURL());
  }

  @Operation(summary = "Get application on yarn name")
  @PostMapping("name")
  public RestResponse yarnName(Application app) {
    String yarnName = applicationInfoService.getYarnName(app);
    return RestResponse.success(yarnName);
  }

  @Operation(summary = "Check the application exist status")
  @PostMapping("checkName")
  public RestResponse checkName(Application app) {
    AppExistsStateEnum exists = applicationInfoService.checkExists(app);
    return RestResponse.success(exists.get());
  }

  @Operation(summary = "Get application conf")
  @PostMapping("readConf")
  public RestResponse readConf(Application app) throws IOException {
    String config = applicationInfoService.readConf(app);
    return RestResponse.success(config);
  }

  @Operation(summary = "Get application main-class")
  @PostMapping("main")
  public RestResponse getMain(Application application) {
    String mainClass = applicationInfoService.getMain(application);
    return RestResponse.success(mainClass);
  }

  @Operation(summary = "List application backups")
  @PostMapping("backups")
  public RestResponse backups(ApplicationBackUp backUp, RestRequest request) {
    IPage<ApplicationBackUp> backups = backUpService.getPage(backUp, request);
    return RestResponse.success(backups);
  }

  @Operation(summary = "List application operation logs")
  @PostMapping("optionlog")
  public RestResponse optionlog(ApplicationLog applicationLog, RestRequest request) {
    IPage<ApplicationLog> applicationList = applicationLogService.getPage(applicationLog, request);
    return RestResponse.success(applicationList);
  }

  @Operation(summary = "Delete application operation log")
  @PermissionAction(id = "#applicationLog.appId", type = PermissionTypeEnum.APP)
  @PostMapping("deleteOperationLog")
  @RequiresPermissions("app:delete")
  public RestResponse deleteOperationLog(Long id) {
    Boolean deleted = applicationLogService.removeById(id);
    return RestResponse.success(deleted);
  }

  @Operation(summary = "Delete application")
  @PermissionAction(id = "#app.id", type = PermissionTypeEnum.APP)
  @PostMapping("delete")
  @RequiresPermissions("app:delete")
  public RestResponse delete(Application app) throws InternalException {
    Boolean deleted = applicationManageService.remove(app);
    return RestResponse.success(deleted);
  }

  @Operation(summary = "Backup application when deleted")
  @PermissionAction(id = "#backUp.appId", type = PermissionTypeEnum.APP)
  @PostMapping("deletebak")
  public RestResponse deleteBak(ApplicationBackUp backUp) throws InternalException {
    Boolean deleted = backUpService.removeById(backUp.getId());
    return RestResponse.success(deleted);
  }

  @Operation(summary = "Check the application jar")
  @PostMapping("checkjar")
  public RestResponse checkjar(String jar) {
    File file = new File(jar);
    try {
      Utils.checkJarFile(file.toURI().toURL());
      return RestResponse.success(true);
    } catch (IOException e) {
      return RestResponse.success(file).message(e.getLocalizedMessage());
    }
  }

  @Operation(summary = "Upload the application jar")
  @PostMapping("upload")
  @RequiresPermissions("app:create")
  public RestResponse upload(MultipartFile file) throws Exception {
    String uploadPath = resourceService.upload(file);
    return RestResponse.success(uploadPath);
  }

  @Hidden
  @PostMapping("verifySchema")
  public RestResponse verifySchema(String path) {
    final URI uri = URI.create(path);
    final String scheme = uri.getScheme();
    final String pathPart = uri.getPath();
    RestResponse restResponse = RestResponse.success(true);
    String error = null;
    if (scheme == null) {
      error =
          "The scheme (hdfs://, file://, etc) is null. Please specify the file system scheme explicitly in the URI.";
    } else if (pathPart == null) {
      error =
          "The path to store the checkpoint data in is null. Please specify a directory path for the checkpoint data.";
    } else if (pathPart.isEmpty() || "/".equals(pathPart)) {
      error = "Cannot use the root directory for checkpoints.";
    }
    if (error != null) {
      restResponse = RestResponse.success(false).message(error);
    }
    return restResponse;
  }

  @Operation(summary = "Check the application savepoint path")
  @PostMapping("checkSavepointPath")
  public RestResponse checkSavepointPath(Application app) throws Exception {
    String error = applicationInfoService.checkSavepointPath(app);
    if (error == null) {
      return RestResponse.success(true);
    }
    return RestResponse.success(false).message(error);
  }

  @Operation(summary = "Get application on k8s deploy logs")
  @Parameters({
    @Parameter(
        name = "id",
        description = "app id",
        required = true,
        example = "100000",
        schema = @Schema(implementation = Long.class)),
    @Parameter(
        name = "offset",
        description = "number of log lines offset",
        required = true,
        example = "0",
        schema = @Schema(implementation = int.class)),
    @Parameter(
        name = "limit",
        description = "number of log lines loaded at once",
        required = true,
        example = "100",
        schema = @Schema(implementation = int.class)),
  })
  @PostMapping(value = "k8sStartLog")
  public RestResponse k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
    String resp = applicationInfoService.k8sStartLog(id, offset, limit);
    return RestResponse.success(resp);
  }
}
