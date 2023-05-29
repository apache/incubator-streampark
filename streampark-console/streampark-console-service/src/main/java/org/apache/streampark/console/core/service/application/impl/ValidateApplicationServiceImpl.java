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

package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.runner.EnvInitializer;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.application.ValidateApplicationService;
import org.apache.streampark.console.core.task.FlinkRESTAPIWatcher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Slf4j
@Service
@Lazy
public class ValidateApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ValidateApplicationService {

  @Autowired @Lazy private FlinkEnvService flinkEnvService;
  @Autowired @Lazy private EnvInitializer envInitializer;
  @Autowired @Lazy private FlinkClusterService flinkClusterService;

  @PostConstruct
  public void resetOptionState() {
    this.baseMapper.resetOptionState();
  }

  @Override
  public boolean existsByTeamId(Long teamId) {
    return baseMapper.existsByTeamId(teamId);
  }

  @Override
  public boolean existsRunningJobByClusterId(Long clusterId) {
    boolean exists = baseMapper.existsRunningJobByClusterId(clusterId);
    if (exists) {
      return true;
    }
    for (Application application : FlinkRESTAPIWatcher.getWatchingApps()) {
      if (clusterId.equals(application.getFlinkClusterId())
          && FlinkAppState.RUNNING.equals(application.getFlinkAppStateEnum())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean existsJobByClusterId(Long clusterId) {
    return baseMapper.existsJobByClusterId(clusterId);
  }

  @Override
  public boolean existsByJobName(String jobName) {
    return this.baseMapper.existsByJobName(jobName);
  }

  @Override
  public boolean existsJobByFlinkEnvId(Long flinkEnvId) {
    LambdaQueryWrapper<Application> lambdaQueryWrapper =
        new LambdaQueryWrapper<Application>().eq(Application::getVersionId, flinkEnvId);
    return getBaseMapper().exists(lambdaQueryWrapper);
  }

  @Override
  public boolean existsByUserId(Long userId) {
    return baseMapper.existsByUserId(userId);
  }

  @Override
  public boolean checkEnv(Application appParam) throws ApplicationException {
    Application application = getById(appParam.getId());
    try {
      FlinkEnv flinkEnv;
      if (application.getVersionId() != null) {
        flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
      } else {
        flinkEnv = flinkEnvService.getDefault();
      }
      if (flinkEnv == null) {
        return false;
      }
      envInitializer.checkFlinkEnv(application.getStorageType(), flinkEnv);
      envInitializer.storageInitialize(application.getStorageType());

      if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())
          || ExecutionMode.REMOTE.equals(application.getExecutionModeEnum())) {
        FlinkCluster flinkCluster = flinkClusterService.getById(application.getFlinkClusterId());
        boolean conned = flinkCluster.verifyClusterConnection();
        if (!conned) {
          throw new ApiAlertException("the target cluster is unavailable, please check!");
        }
      }
      return true;
    } catch (Exception e) {
      log.error(Utils.stringifyException(e));
      throw new ApiDetailException(e);
    }
  }

  @Override
  public boolean checkAlter(Application application) {
    Long appId = application.getId();
    FlinkAppState state = FlinkAppState.of(application.getState());
    if (!FlinkAppState.CANCELED.equals(state)) {
      return false;
    }
    long cancelUserId = FlinkRESTAPIWatcher.getCanceledJobUserId(appId);
    long appUserId = application.getUserId();
    return cancelUserId != -1 && cancelUserId != appUserId;
  }
}
