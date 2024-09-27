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
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

/** Flink Cluster Service, Provides control over the cluster */
public interface FlinkClusterService extends IService<FlinkCluster> {

    /**
     * List all currently available clusters
     *
     * @return List of flink cluster
     */
    List<FlinkCluster> listAvailableCluster();

    /**
     * Check the flink cluster status
     *
     * @param flinkCluster FlinkCluster To be check
     * @return The response value
     */
    ResponseResult check(FlinkCluster flinkCluster);

    /**
     * Create flink cluster
     *
     * @param flinkCluster FlinkCluster to be create
     * @return Whether the creation is successful
     */
    Boolean create(FlinkCluster flinkCluster, Long userId);

    /**
     * Remove flink cluster
     *
     * @param id FlinkCluster id whitch to be removed
     */
    void remove(Long id);

    /**
     * Update flink cluster
     *
     * @param flinkCluster FlinkCluster to be update
     */
    void update(FlinkCluster flinkCluster);

    /**
     * Start flink cluster
     *
     * @param flinkCluster FlinkCluster to be start
     */
    void start(FlinkCluster flinkCluster);

    /**
     * Shutdown flink cluster
     *
     * @param flinkCluster to be shutdown
     */
    void shutdown(FlinkCluster flinkCluster);

    /**
     * Allow to shut down flink cluster
     *
     * @param flinkCluster FlinkCluster can be shutdown now
     * @return Whether the operation was successful
     */
    Boolean allowShutdownCluster(FlinkCluster flinkCluster);

    /**
     * Query whether the Flink cluster with the specified cluster id exists
     *
     * @param clusterId target cluster id
     * @param id Current flink cluster id
     * @return Whether the cluster exists
     */
    Boolean existsByClusterId(String clusterId, Long id);

    /**
     * Query whether the Flink cluster with the specified cluster id exists
     *
     * @param clusterName target cluster name
     * @param id Current flink cluster id
     * @return Whether the cluster exists
     */
    Boolean existsByClusterName(String clusterName, Long id);

    /**
     * Query whether the Flink cluster with the specified FlinkEnv id exists
     *
     * @param id FlinkEnv id
     * @return Whether the cluster exists
     */
    Boolean existsByFlinkEnvId(Long id);

    /**
     * Lists the corresponding flink clusters based on DeployMode
     *
     * @param deployModeEnums Collection of FlinkDeployMode
     * @return List of flink cluster
     */
    List<FlinkCluster> listByDeployModes(Collection<FlinkDeployMode> deployModeEnums);

    /**
     * update flink cluster state
     *
     * @param id flink cluster id
     * @param state flink cluster state
     */
    void updateClusterState(Long id, ClusterState state);

    IPage<FlinkCluster> findPage(FlinkCluster flinkCluster, RestRequest restRequest);
}
