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
import org.apache.streampark.console.core.annotation.PermissionScope;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Savepoint;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.SavepointService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;

@Slf4j
@Validated
@RestController
@RequestMapping("flink/savepoint")
public class SavepointController {

  @Autowired private ApplicationService applicationService;

  @Autowired private SavepointService savepointService;

  @PostMapping("history")
  @PermissionScope(app = "#sp.appId", team = "#sp.teamId")
  public RestResponse history(Savepoint sp, RestRequest request) {
    IPage<Savepoint> page = savepointService.page(sp, request);
    return RestResponse.success(page);
  }

  @PostMapping("delete")
  @RequiresPermissions("savepoint:delete")
  @PermissionScope(app = "#sp.appId", team = "#sp.teamId")
  public RestResponse delete(Savepoint sp) throws InternalException {
    Savepoint savepoint = savepointService.getById(sp.getId());
    Application application = applicationService.getById(savepoint.getAppId());
    Boolean deleted = savepointService.delete(sp.getId(), application);
    return RestResponse.success(deleted);
  }

  @PostMapping("trigger")
  @RequiresPermissions("savepoint:trigger")
  @PermissionScope(app = "#savepoint.appId", team = "#savepoint.teamId")
  public RestResponse trigger(Savepoint savepoint, @Nullable String savepointPath)
      throws Exception {
    savepointService.trigger(savepoint.getAppId(), savepointPath);
    return RestResponse.success(true);
  }
}
