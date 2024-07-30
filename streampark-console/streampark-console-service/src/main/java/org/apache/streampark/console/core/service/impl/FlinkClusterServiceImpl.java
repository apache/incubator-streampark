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
import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.YarnQueueService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.watcher.FlinkClusterWatcher;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.DeployRequest;
import org.apache.streampark.flink.client.bean.DeployResponse;
import org.apache.streampark.flink.client.bean.KubernetesDeployParam;
import org.apache.streampark.flink.client.bean.ShutDownRequest;
import org.apache.streampark.flink.client.bean.ShutDownResponse;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.apache.streampark.console.base.enums.MessageStatus.APP_EXECUTE_MODE_OPERATION_DISABLE_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.APP_QUEUE_LABEL_IN_DATABASE_ILLEGALLY;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_CLOSE_FAILED;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_DELETE_RUNNING_CLUSTER_FAILED;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_DEPLOY_FAILED;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_EXIST_APP_DELETE_FAILED;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_EXIST_RUN_TASK_CLOSE_FAILED;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_ID_EMPTY_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_NOT_EXIST;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_NOT_RUNNING;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_CLUSTER_SHUTDOWN_RESPONSE_FAILED;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkClusterServiceImpl extends ServiceImpl<FlinkClusterMapper, FlinkCluster>
    implements
        FlinkClusterService {

    @Qualifier("streamparkClusterExecutor")
    @Autowired
    private ExecutorService executorService;

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private ApplicationInfoService applicationInfoService;

    @Autowired
    private YarnQueueService yarnQueueService;

    @Autowired
    private FlinkClusterWatcher flinkClusterWatcher;

    @Override
    public List<FlinkCluster> listAvailableCluster() {
        LambdaQueryWrapper<FlinkCluster> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FlinkCluster::getClusterState, ClusterState.RUNNING);
        return this.list(lambdaQueryWrapper);
    }

    @Override
    public ResponseResult check(FlinkCluster cluster) {
        ResponseResult result = new ResponseResult();
        result.setStatus(0);

        // 1) Check name if already exists
        Boolean existsByClusterName = this.existsByClusterName(cluster.getClusterName(), cluster.getId());
        if (existsByClusterName) {
            result.setMsg("ClusterName already exists, please check!");
            result.setStatus(1);
            return result;
        }

        // 2) Check target-cluster if already exists
        String clusterId = cluster.getClusterId();
        if (StringUtils.isNotBlank(clusterId) && this.existsByClusterId(clusterId, cluster.getId())) {
            result.setMsg("The clusterId " + clusterId + " already exists,please check!");
            result.setStatus(2);
            return result;
        }

        // 3) Check connection
        if (FlinkExecutionMode.isRemoteMode(cluster.getFlinkExecutionModeEnum())
            && cluster.getClusterId() != null
            && !flinkClusterWatcher.verifyClusterConnection(cluster)) {
            result.setMsg("The remote cluster connection failed, please check!");
            result.setStatus(3);
            return result;
        }
        if (FlinkExecutionMode.isYarnMode(cluster.getFlinkExecutionModeEnum())
            && cluster.getClusterId() != null
            && !flinkClusterWatcher.verifyClusterConnection(cluster)) {
            result.setMsg("The flink cluster connection failed, please check!");
            result.setStatus(4);
            return result;
        }

        return result;
    }

    @Override
    public Boolean create(FlinkCluster flinkCluster, Long userId) {
        flinkCluster.setUserId(userId);
        return internalCreate(flinkCluster);
    }

    @VisibleForTesting
    public boolean internalCreate(FlinkCluster flinkCluster) {
        boolean successful = validateQueueIfNeeded(flinkCluster);
        ApiAlertException.throwIfFalse(
            successful, APP_QUEUE_LABEL_IN_DATABASE_ILLEGALLY, flinkCluster.getYarnQueue());
        flinkCluster.setCreateTime(new Date());
        if (FlinkExecutionMode.isRemoteMode(flinkCluster.getFlinkExecutionModeEnum())) {
            flinkCluster.setClusterState(ClusterState.RUNNING.getState());
            flinkCluster.setStartTime(new Date());
            flinkCluster.setEndTime(null);
        } else {
            flinkCluster.setClusterState(ClusterState.CREATED.getState());
        }
        boolean ret = save(flinkCluster);
        if (ret && FlinkExecutionMode.isRemoteMode(flinkCluster.getExecutionMode())) {
            FlinkClusterWatcher.addWatching(flinkCluster);
        }
        return ret;
    }

    @Override
    public void start(FlinkCluster cluster) {
        FlinkCluster flinkCluster = getById(cluster.getId());
        try {
            DeployResponse deployResponse = deployInternal(flinkCluster);
            ApiAlertException.throwIfNull(
                deployResponse, FLINK_CLUSTER_DEPLOY_FAILED);
            if (FlinkExecutionMode.isYarnSessionMode(flinkCluster.getFlinkExecutionModeEnum())) {
                String address = String.format(
                    "%s/proxy/%s/", YarnUtils.getRMWebAppURL(true), deployResponse.clusterId());
                flinkCluster.setAddress(address);
                flinkCluster.setJobManagerUrl(deployResponse.address());
            } else {
                flinkCluster.setAddress(deployResponse.address());
            }
            flinkCluster.setClusterId(deployResponse.clusterId());
            flinkCluster.setClusterState(ClusterState.RUNNING.getState());
            flinkCluster.setException(null);
            flinkCluster.setEndTime(null);
            updateById(flinkCluster);
            FlinkClusterWatcher.addWatching(flinkCluster);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            flinkCluster.setClusterState(ClusterState.FAILED.getState());
            flinkCluster.setException(e.toString());
            updateById(flinkCluster);
            throw new ApiDetailException(e);
        }
    }

    @Override
    public void update(FlinkCluster paramOfCluster) {
        FlinkCluster flinkCluster = getById(paramOfCluster.getId());
        boolean success = validateQueueIfNeeded(flinkCluster, paramOfCluster);
        ApiAlertException.throwIfFalse(
            success, APP_QUEUE_LABEL_IN_DATABASE_ILLEGALLY, paramOfCluster.getYarnQueue());

        flinkCluster.setClusterName(paramOfCluster.getClusterName());
        flinkCluster.setAlertId(paramOfCluster.getAlertId());
        flinkCluster.setDescription(paramOfCluster.getDescription());
        if (FlinkExecutionMode.isRemoteMode(flinkCluster.getFlinkExecutionModeEnum())) {
            updateFlinkClusterForRemoteMode(paramOfCluster, flinkCluster);
            FlinkClusterWatcher.addWatching(flinkCluster);
        } else {
            updateFlinkClusterForNonRemoteModes(paramOfCluster, flinkCluster);
        }
        updateById(flinkCluster);
    }

    private void updateFlinkClusterForNonRemoteModes(
                                                     FlinkCluster paramOfCluster, FlinkCluster flinkCluster) {
        flinkCluster.setClusterId(paramOfCluster.getClusterId());
        flinkCluster.setVersionId(paramOfCluster.getVersionId());
        flinkCluster.setDynamicProperties(paramOfCluster.getDynamicProperties());
        flinkCluster.setOptions(paramOfCluster.getOptions());
        flinkCluster.setResolveOrder(paramOfCluster.getResolveOrder());
        flinkCluster.setK8sHadoopIntegration(paramOfCluster.getK8sHadoopIntegration());
        flinkCluster.setK8sConf(paramOfCluster.getK8sConf());
        flinkCluster.setK8sNamespace(paramOfCluster.getK8sNamespace());
        flinkCluster.setK8sRestExposedType(paramOfCluster.getK8sRestExposedType());
        flinkCluster.setServiceAccount(paramOfCluster.getServiceAccount());
        flinkCluster.setFlinkImage(paramOfCluster.getFlinkImage());
        flinkCluster.setYarnQueue(paramOfCluster.getYarnQueue());
    }

    private void updateFlinkClusterForRemoteMode(
                                                 FlinkCluster paramOfCluster, FlinkCluster flinkCluster) {
        flinkCluster.setAddress(paramOfCluster.getAddress());
        flinkCluster.setClusterState(ClusterState.RUNNING.getState());
        flinkCluster.setStartTime(new Date());
        flinkCluster.setEndTime(null);
    }

    @Override
    public void shutdown(FlinkCluster cluster) {
        FlinkCluster flinkCluster = this.getById(cluster.getId());

        try {
            ShutDownResponse shutDownResponse = shutdownInternal(flinkCluster, flinkCluster.getClusterId());
            ApiAlertException.throwIfNull(shutDownResponse, FLINK_CLUSTER_SHUTDOWN_RESPONSE_FAILED);
            flinkCluster.setClusterState(ClusterState.CANCELED.getState());
            flinkCluster.setEndTime(new Date());
            updateById(flinkCluster);
            FlinkClusterWatcher.unWatching(flinkCluster);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            flinkCluster.setException(e.toString());
            updateById(flinkCluster);
            throw new ApiDetailException(FLINK_CLUSTER_CLOSE_FAILED, e);
        }
    }

    public Boolean allowShutdownCluster(FlinkCluster cluster) {
        FlinkCluster flinkCluster = this.getById(cluster.getId());
        // 1) check mode
        String clusterId = flinkCluster.getClusterId();
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(clusterId), FLINK_CLUSTER_ID_EMPTY_ERROR);

        // 2) check cluster is active
        checkActiveIfNeeded(flinkCluster);

        // 3) check job if running on cluster
        boolean existsRunningJob = applicationInfoService.existsRunningByClusterId(flinkCluster.getId());
        ApiAlertException.throwIfTrue(
            existsRunningJob, FLINK_CLUSTER_EXIST_RUN_TASK_CLOSE_FAILED);
        return true;
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
    public Boolean existsByFlinkEnvId(Long flinkEnvId) {
        LambdaQueryWrapper<FlinkCluster> lambdaQueryWrapper = new LambdaQueryWrapper<FlinkCluster>()
            .eq(FlinkCluster::getVersionId, flinkEnvId);
        return getBaseMapper().exists(lambdaQueryWrapper);
    }

    @Override
    public List<FlinkCluster> listByExecutionModes(
                                                   Collection<FlinkExecutionMode> executionModeEnums) {
        return getBaseMapper()
            .selectList(
                new LambdaQueryWrapper<FlinkCluster>()
                    .in(
                        FlinkCluster::getExecutionMode,
                        executionModeEnums.stream()
                            .map(FlinkExecutionMode::getMode)
                            .collect(Collectors.toSet())));
    }

    @Override
    public void updateClusterState(Long id, ClusterState state) {
        LambdaUpdateWrapper<FlinkCluster> updateWrapper = new LambdaUpdateWrapper<FlinkCluster>()
            .eq(FlinkCluster::getId, id)
            .set(FlinkCluster::getClusterState, state.getState());

        switch (state) {
            case KILLED:
            case UNKNOWN:
            case LOST:
            case FAILED:
            case CANCELED:
                updateWrapper.set(FlinkCluster::getEndTime, new Date());
                break;
            case STARTING:
                updateWrapper.set(FlinkCluster::getStartTime, new Date());
                break;
            default:
                break;
        }

        update(updateWrapper);
    }

    @Override
    public void remove(Long id) {
        FlinkCluster flinkCluster = getById(id);
        ApiAlertException.throwIfNull(flinkCluster, FLINK_CLUSTER_NOT_EXIST);

        if (FlinkExecutionMode.isYarnSessionMode(flinkCluster.getFlinkExecutionModeEnum())
            || FlinkExecutionMode.isKubernetesSessionMode(flinkCluster.getExecutionMode())) {
            ApiAlertException.throwIfTrue(
                ClusterState.isRunning(flinkCluster.getClusterStateEnum()),
                FLINK_CLUSTER_DELETE_RUNNING_CLUSTER_FAILED);
        }
        ApiAlertException.throwIfTrue(
            applicationInfoService.existsByClusterId(id),
            FLINK_CLUSTER_EXIST_APP_DELETE_FAILED);
        removeById(id);
    }

    /**
     * Check queue label validation when create the cluster if needed.
     *
     * @param clusterInfo the new cluster info.
     * @return <code>true</code> if validate it successfully, <code>false</code> else.
     */
    @VisibleForTesting
    public boolean validateQueueIfNeeded(FlinkCluster clusterInfo) {
        yarnQueueService.checkQueueLabel(
            clusterInfo.getFlinkExecutionModeEnum(), clusterInfo.getYarnQueue());
        if (!isYarnNotDefaultQueue(clusterInfo)) {
            return true;
        }
        return yarnQueueService.existByQueueLabel(clusterInfo.getYarnQueue());
    }

    /**
     * Check queue label validation when update the cluster if needed.
     *
     * @param oldCluster the old cluster.
     * @param newCluster the new cluster.
     * @return <code>true</code> if validate it successfully, <code>false</code> else.
     */
    @VisibleForTesting
    public boolean validateQueueIfNeeded(FlinkCluster oldCluster, FlinkCluster newCluster) {
        yarnQueueService.checkQueueLabel(
            newCluster.getFlinkExecutionModeEnum(), newCluster.getYarnQueue());
        if (!isYarnNotDefaultQueue(newCluster)) {
            return true;
        }

        if (FlinkExecutionMode.isYarnSessionMode(newCluster.getFlinkExecutionModeEnum())
            && StringUtils.equals(oldCluster.getYarnQueue(), newCluster.getYarnQueue())) {
            return true;
        }
        return yarnQueueService.existByQueueLabel(newCluster.getYarnQueue());
    }

    /**
     * Judge the execution mode whether is the Yarn session mode with not default or empty queue
     * label.
     *
     * @param cluster cluster.
     * @return If the executionMode is yarn session mode and the queue label is not (empty or
     * default), return true, false else.
     */
    private boolean isYarnNotDefaultQueue(FlinkCluster cluster) {
        return FlinkExecutionMode.isYarnSessionMode(cluster.getFlinkExecutionModeEnum())
            && !yarnQueueService.isDefaultQueue(cluster.getYarnQueue());
    }

    private ShutDownResponse shutdownInternal(FlinkCluster flinkCluster,
                                              String clusterId) throws InterruptedException, ExecutionException, TimeoutException {
        ShutDownRequest stopRequest = new ShutDownRequest(
            flinkEnvService.getById(flinkCluster.getVersionId()).getFlinkVersion(),
            flinkCluster.getFlinkExecutionModeEnum(),
            flinkCluster.getProperties(),
            clusterId,
            flinkCluster.getId(),
            getKubernetesDeployDesc(flinkCluster, "shutdown"));
        Future<ShutDownResponse> future = executorService.submit(() -> FlinkClient.shutdown(stopRequest));
        return future.get(60, TimeUnit.SECONDS);
    }

    private DeployResponse deployInternal(FlinkCluster flinkCluster) throws InterruptedException, ExecutionException, TimeoutException {
        DeployRequest deployRequest = new DeployRequest(
            flinkEnvService.getById(flinkCluster.getVersionId()).getFlinkVersion(),
            flinkCluster.getFlinkExecutionModeEnum(),
            flinkCluster.getProperties(),
            flinkCluster.getClusterId(),
            flinkCluster.getId(),
            getKubernetesDeployDesc(flinkCluster, "start"));
        log.info("Deploy cluster request {}", deployRequest);
        Future<DeployResponse> future = executorService.submit(() -> FlinkClient.deploy(deployRequest));
        return future.get(60, TimeUnit.SECONDS);
    }

    private void checkActiveIfNeeded(FlinkCluster flinkCluster) {
        if (FlinkExecutionMode.isYarnSessionMode(flinkCluster.getFlinkExecutionModeEnum())) {
            ApiAlertException.throwIfFalse(
                ClusterState.isRunning(flinkCluster.getClusterStateEnum()),
                FLINK_CLUSTER_NOT_RUNNING);
            if (!flinkClusterWatcher.verifyClusterConnection(flinkCluster)) {
                flinkCluster.setClusterState(ClusterState.LOST.getState());
                updateById(flinkCluster);
                ApiAlertException.throwException(FLINK_CLUSTER_NOT_RUNNING);
            }
        }
    }

    @Nullable
    private KubernetesDeployParam getKubernetesDeployDesc(
                                                          @Nonnull FlinkCluster flinkCluster, String action) {
        FlinkExecutionMode executionModeEnum = flinkCluster.getFlinkExecutionModeEnum();
        switch (executionModeEnum) {
            case YARN_SESSION:
                break;
            case KUBERNETES_NATIVE_SESSION:
                return new KubernetesDeployParam(
                    flinkCluster.getClusterId(),
                    flinkCluster.getK8sNamespace(),
                    flinkCluster.getK8sConf(),
                    flinkCluster.getServiceAccount(),
                    flinkCluster.getFlinkImage(),
                    flinkCluster.getK8sRestExposedTypeEnum());
            default:
                ApiAlertException.throwException(
                    APP_EXECUTE_MODE_OPERATION_DISABLE_ERROR, executionModeEnum.getName(),
                    action);
        }
        return null;
    }
}
