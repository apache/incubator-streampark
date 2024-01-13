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

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

public interface FlinkClusterService extends IService<FlinkCluster> {

  List<FlinkCluster> listAvailableCluster();

  ResponseResult check(FlinkCluster flinkCluster);

  Boolean create(FlinkCluster flinkCluster);

  void remove(Long id);

  void update(FlinkCluster flinkCluster);

  void start(FlinkCluster flinkCluster);

  void shutdown(FlinkCluster flinkCluster);

  Boolean allowShutdownCluster(FlinkCluster flinkCluster);

  Boolean existsByClusterId(String clusterId, Long id);

  Boolean existsByClusterName(String clusterName, Long id);

  Boolean existsByFlinkEnvId(Long id);

  List<FlinkCluster> listByExecutionModes(Collection<FlinkExecutionMode> executionModeEnums);

  void updateClusterState(Long id, ClusterState state);
}
