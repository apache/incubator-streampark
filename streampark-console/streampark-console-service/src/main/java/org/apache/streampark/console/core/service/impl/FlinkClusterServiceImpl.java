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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.task.FlinkTrackingTask;
import org.apache.streampark.flink.submit.FlinkSubmitter;
import org.apache.streampark.flink.submit.bean.DeployRequest;
import org.apache.streampark.flink.submit.bean.DeployResponse;
import org.apache.streampark.flink.submit.bean.KubernetesDeployParam;
import org.apache.streampark.flink.submit.bean.ShutDownRequest;
import org.apache.streampark.flink.submit.bean.ShutDownResponse;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkClusterServiceImpl extends ServiceImpl<FlinkClusterMapper, FlinkCluster> implements FlinkClusterService {

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 5,
        Runtime.getRuntime().availableProcessors() * 10,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streampark-cluster-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private CommonService commonService;


    @Autowired
    private ApplicationService applicationService;

    @Override
    public ResponseResult check(FlinkCluster cluster) {
        ResponseResult result = new ResponseResult();
        result.setStatus(0);

        //1) Check name is already exists
        Boolean existsByClusterName = this.existsByClusterName(cluster.getClusterName(), cluster.getId());
        if (existsByClusterName) {
            result.setMsg("clusterName is already exists,please check!");
            result.setStatus(1);
            return result;
        }

        //2) Check target-cluster is already exists
        String clusterId = cluster.getClusterId();
        if (StringUtils.isNotEmpty(clusterId)) {
            Boolean existsByClusterId = this.existsByClusterId(clusterId, cluster.getId());
            if (existsByClusterId) {
                result.setMsg("the clusterId " + clusterId + " is already exists,please check!");
                result.setStatus(2);
                return result;
            }
        }

        // 3) Check connection
        if (ExecutionMode.REMOTE.equals(cluster.getExecutionModeEnum())) {
            if (!cluster.verifyClusterConnection()) {
                result.setMsg("the remote cluster connection failed, please check!");
                result.setStatus(3);
                return result;
            }
        } else if (ExecutionMode.YARN_SESSION.equals(cluster.getExecutionModeEnum()) && cluster.getClusterId() != null) {
            if (!cluster.verifyClusterConnection()) {
                result.setMsg("the flink cluster connection failed, please check!");
                result.setStatus(4);
                return result;
            }
        }

        return result;
    }

    @Override
    public Boolean create(FlinkCluster flinkCluster) {
        flinkCluster.setUserId(commonService.getUserId());
        flinkCluster.setCreateTime(new Date());
        if (ExecutionMode.REMOTE.equals(flinkCluster.getExecutionModeEnum())) {
            flinkCluster.setClusterState(ClusterState.STARTED.getValue());
        } else {
            flinkCluster.setClusterState(ClusterState.CREATED.getValue());
        }
        return save(flinkCluster);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void start(FlinkCluster cluster) {
        FlinkCluster flinkCluster = getById(cluster.getId());
        LambdaUpdateWrapper<FlinkCluster> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(FlinkCluster::getId, flinkCluster.getId());
        try {
            ExecutionMode executionModeEnum = flinkCluster.getExecutionModeEnum();
            KubernetesDeployParam kubernetesDeployParam = null;
            switch (executionModeEnum) {
                case YARN_SESSION:
                    break;
                case KUBERNETES_NATIVE_SESSION:
                    kubernetesDeployParam = new KubernetesDeployParam(
                        flinkCluster.getClusterId(),
                        flinkCluster.getK8sNamespace(),
                        flinkCluster.getK8sConf(),
                        flinkCluster.getServiceAccount(),
                        flinkCluster.getFlinkImage(),
                        flinkCluster.getK8sRestExposedTypeEnum());
                    break;
                default:
                    throw new ApiAlertException("the ExecutionModeEnum " + executionModeEnum.getName() + "can't start!");
            }
            FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());
            DeployRequest deployRequest = new DeployRequest(
                flinkEnv.getFlinkVersion(),
                flinkCluster.getClusterId(),
                executionModeEnum,
                flinkCluster.getProperties(),
                kubernetesDeployParam
            );
            log.info("deploy cluster request " + deployRequest);
            Future<DeployResponse> future = executorService.submit(() -> FlinkSubmitter.deploy(deployRequest));
            DeployResponse deployResponse = future.get(60, TimeUnit.SECONDS);
            if (deployResponse != null) {
                if (ExecutionMode.YARN_SESSION.equals(executionModeEnum)) {
                    String address = YarnUtils.getRMWebAppURL() + "/proxy/" + deployResponse.clusterId() + "/";
                    updateWrapper.set(FlinkCluster::getAddress, address);
                }
                updateWrapper.set(FlinkCluster::getClusterId, deployResponse.clusterId());
                updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STARTED.getValue());
                updateWrapper.set(FlinkCluster::getException, null);
                update(updateWrapper);
                FlinkTrackingTask.removeFlinkCluster(flinkCluster);
            } else {
                throw new ApiAlertException("deploy cluster failed, unknown reason，please check you params or StreamPark error log");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STOPED.getValue());
            updateWrapper.set(FlinkCluster::getException, e.toString());
            update(updateWrapper);
            throw new ApiDetailException(e);
        }
    }

    @Override
    public void update(FlinkCluster cluster) {
        FlinkCluster flinkCluster = getById(cluster.getId());
        flinkCluster.setClusterName(cluster.getClusterName());
        flinkCluster.setDescription(cluster.getDescription());
        if (ExecutionMode.REMOTE.equals(flinkCluster.getExecutionModeEnum())) {
            flinkCluster.setAddress(cluster.getAddress());
        } else {
            flinkCluster.setAddress(null);
            flinkCluster.setClusterId(cluster.getClusterId());
            flinkCluster.setVersionId(cluster.getVersionId());
            flinkCluster.setDynamicProperties(cluster.getDynamicProperties());
            flinkCluster.setOptions(cluster.getOptions());
            flinkCluster.setResolveOrder(cluster.getResolveOrder());
            flinkCluster.setK8sHadoopIntegration(cluster.getK8sHadoopIntegration());
            flinkCluster.setK8sConf(cluster.getK8sConf());
            flinkCluster.setK8sNamespace(cluster.getK8sNamespace());
            flinkCluster.setK8sRestExposedType(cluster.getK8sRestExposedType());
            flinkCluster.setServiceAccount(cluster.getServiceAccount());
            flinkCluster.setFlinkImage(cluster.getFlinkImage());
            flinkCluster.setYarnQueue(cluster.getYarnQueue());
        }
        try {
            updateById(flinkCluster);
        } catch (Exception e) {
            throw new ApiDetailException("update cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public void shutdown(FlinkCluster cluster) {
        FlinkCluster flinkCluster = this.getById(cluster.getId());
        //1) check mode
        ExecutionMode executionModeEnum = flinkCluster.getExecutionModeEnum();
        String clusterId = flinkCluster.getClusterId();
        KubernetesDeployParam kubernetesDeployParam = null;
        switch (executionModeEnum) {
            case YARN_SESSION:
                break;
            case KUBERNETES_NATIVE_SESSION:
                kubernetesDeployParam = new KubernetesDeployParam(
                    flinkCluster.getClusterId(),
                    flinkCluster.getK8sNamespace(),
                    flinkCluster.getK8sConf(),
                    flinkCluster.getServiceAccount(),
                    flinkCluster.getFlinkImage(),
                    flinkCluster.getK8sRestExposedTypeEnum());
                break;
            default:
                throw new ApiAlertException("the ExecutionModeEnum " + executionModeEnum.getName() + "can't shutdown!");
        }
        if (StringUtils.isBlank(clusterId)) {
            throw new ApiAlertException("the clusterId can not be empty!");
        }

        LambdaUpdateWrapper<FlinkCluster> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(FlinkCluster::getId, flinkCluster.getId());

        //2) check cluster is active
        if (ExecutionMode.YARN_SESSION.equals(executionModeEnum) || ExecutionMode.REMOTE.equals(executionModeEnum)) {
            if (ClusterState.STARTED.equals(ClusterState.of(flinkCluster.getClusterState()))) {
                if (!flinkCluster.verifyClusterConnection()) {
                    updateWrapper.set(FlinkCluster::getAddress, null);
                    updateWrapper.set(FlinkCluster::getClusterState, ClusterState.LOST.getValue());
                    update(updateWrapper);
                    throw new ApiAlertException("current cluster is not active, please check");
                }
            } else {
                throw new ApiAlertException("current cluster is not active, please check");
            }
        }

        //3) check job if running on cluster
        boolean existsRunningJob = applicationService.existsRunningJobByClusterId(flinkCluster.getId());
        if (existsRunningJob) {
            throw new ApiAlertException("some app is running on this cluster, the cluster cannot be shutdown");
        }

        //4) shutdown
        FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());
        ShutDownRequest stopRequest = new ShutDownRequest(
            flinkEnv.getFlinkVersion(),
            executionModeEnum,
            clusterId,
            kubernetesDeployParam,
            flinkCluster.getProperties()
        );

        try {
            Future<ShutDownResponse> future = executorService.submit(() -> FlinkSubmitter.shutdown(stopRequest));
            ShutDownResponse shutDownResponse = future.get(60, TimeUnit.SECONDS);
            if (shutDownResponse != null) {
                updateWrapper.set(FlinkCluster::getAddress, null);
                updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STOPED.getValue());
                update(updateWrapper);
            } else {
                throw new ApiAlertException("get shutdown response failed");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            updateWrapper.set(FlinkCluster::getException, e.toString());
            update(updateWrapper);
            throw new ApiDetailException("shutdown cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public Boolean existsByClusterId(String clusterId, Long id) {
        return this.baseMapper.existsByClusterId(clusterId, id);
    }

    @Override
    public Boolean existsByClusterName(String clusterName, Long id) {
        return this.baseMapper.existsByClusterName(clusterName, id);
    }

    @Override
    public void delete(FlinkCluster cluster) {
        Long id = cluster.getId();
        FlinkCluster flinkCluster = getById(id);
        if (flinkCluster == null) {
            throw new ApiAlertException("flink cluster not exist, please check.");
        }

        if (ExecutionMode.YARN_SESSION.equals(flinkCluster.getExecutionModeEnum()) ||
            ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(flinkCluster.getExecutionModeEnum())) {
            if (ClusterState.STARTED.equals(flinkCluster.getClusterStateEnum())) {
                throw new ApiAlertException("flink cluster is running, cannot be delete, please check.");
            }
        }

        if (applicationService.existsJobByClusterId(id)) {
            throw new ApiAlertException("some app on this cluster, the cluster cannot be delete, please check.");
        }
        removeById(id);
    }

}
