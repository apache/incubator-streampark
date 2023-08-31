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

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.AppExistsState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ApplicationInfoServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationInfoService {

  @Override
  public boolean existsByTeamId(Long teamId) {
    return false;
  }

  @Override
  public boolean existsByUserId(Long userId) {
    return false;
  }

  @Override
  public boolean existsRunningJobByClusterId(Long clusterId) {
    return false;
  }

  @Override
  public boolean existsJobByClusterId(Long clusterId) {
    return false;
  }

  @Override
  public boolean existsJobByFlinkEnvId(Long flinkEnvId) {
    return false;
  }

  @Override
  public Integer countJobsByClusterId(Long clusterId) {
    return null;
  }

  @Override
  public Integer countAffectedJobsByClusterId(Long clusterId, String dbType) {
    return null;
  }

  @Override
  public String getYarnName(Application app) {
    return null;
  }

  @Override
  public AppExistsState checkExists(Application app) {
    return null;
  }

  @Override
  public void persistMetrics(Application application) {}

  @Override
  public String readConf(Application app) throws IOException {
    return null;
  }

  @Override
  public String getMain(Application application) {
    return null;
  }

  @Override
  public Map<String, Serializable> dashboard(Long teamId) {
    return null;
  }

  @Override
  public String k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
    return null;
  }

  @Override
  public List<String> getRecentK8sNamespace() {
    return null;
  }

  @Override
  public List<String> getRecentK8sClusterId(Integer executionMode) {
    return null;
  }

  @Override
  public List<String> getRecentFlinkBaseImage() {
    return null;
  }

  @Override
  public List<String> getRecentK8sPodTemplate() {
    return null;
  }

  @Override
  public List<String> getRecentK8sJmPodTemplate() {
    return null;
  }

  @Override
  public List<String> getRecentK8sTmPodTemplate() {
    return null;
  }

  @Override
  public List<String> historyUploadJars() {
    return null;
  }
}
