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

package org.apache.streampark.console.core.service;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedError;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ProjectService extends IService<Project> {

  RestResponse create(Project project);

  boolean update(Project projectParam);

  boolean delete(Long id);

  IPage<Project> page(Project project, RestRequest restRequest);

  Boolean existsByTeamId(Long teamId);

  List<Project> findByTeamId(Long teamId);

  void build(Long id) throws Exception;

  RestResponse getBuildLog(Long id, Long startOffset);

  List<String> modules(Long id);

  List<String> jars(Project project);

  List<Map<String, Object>> listConf(Project project);

  String getAppConfPath(Long id, String module);

  List<Application> getApplications(Project project);

  boolean checkExists(Project project);

  List<String> getAllBranches(Project project);

  GitAuthorizedError gitCheck(Project project);
}
