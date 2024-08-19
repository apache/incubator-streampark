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

package org.apache.streampark.console.core.mapper;

import org.apache.streampark.console.core.entity.Application;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ApplicationMapper extends BaseMapper<Application> {

  IPage<Application> page(Page<Application> page, @Param("application") Application application);

  Application getApp(@Param("application") Application application);

  void persistMetrics(@Param("application") Application application);

  List<Application> getByTeamId(@Param("teamId") Long teamId);

  boolean mapping(@Param("application") Application appParam);

  List<String> getRecentK8sNamespace(@Param("limitSize") Integer limit);

  List<String> getRecentK8sClusterId(@Param("limitSize") Integer limit);

  List<String> getRecentFlinkBaseImage(@Param("limitSize") Integer limit);

  List<String> getRecentK8sPodTemplate(@Param("limitSize") Integer limit);

  List<String> getRecentK8sJmPodTemplate(@Param("limitSize") Integer limit);

  List<String> getRecentK8sTmPodTemplate(@Param("limitSize") Integer limit);

  void resetOptionState();

  Boolean existsByTeamId(@Param("teamId") Long teamId);

  Boolean existsByJobName(@Param("jobName") String jobName);

  List<Application> getByProjectId(@Param("projectId") Long id);

  boolean existsRunningJobByClusterId(@Param("clusterId") Long clusterId);

  boolean existsJobByClusterId(@Param("clusterId") Long clusterId);

  void updateJobManagerUrl(@Param("id") Long id, @Param("url") String url);
}
