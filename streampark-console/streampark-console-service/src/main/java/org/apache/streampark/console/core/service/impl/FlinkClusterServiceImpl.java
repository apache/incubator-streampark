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
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.YarnQueueService;
import org.apache.streampark.console.core.task.FlinkClusterWatcher;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.DeployRequest;
import org.apache.streampark.flink.client.bean.DeployResponse;
import org.apache.streampark.flink.client.bean.KubernetesDeployParam;
import org.apache.streampark.flink.client.bean.ShutDownRequest;
import org.apache.streampark.flink.client.bean.ShutDownResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkClusterServiceImpl extends ServiceImpl<FlinkClusterMapper, FlinkCluster>
    implements FlinkClusterService {

  private static final String ERROR_CLUSTER_QUEUE_HINT =
      "Queue label '%s' isn't available in database, please add it first.";

  private final ExecutorService executorService =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("streampark-cluster-executor"),
          new ThreadPoolExecutor.AbortPolicy());

  @Autowired private FlinkEnvService flinkEnvService;

  @Autowired private CommonService commonService;

  @Autowired private ApplicationService applicationService;

  @Autowired private YarnQueueService yarnQueueService;

  @Override
  public ResponseResult check(FlinkCluster cluster) {
    ResponseResult result = new ResponseResult();
    result.setStatus(0);

    // 1) Check name is already exists
    Boolean existsByClusterName =
        this.existsByClusterName(cluster.getClusterName(), cluster.getId());
    if (existsByClusterName) {
      result.setMsg("ClusterName is already exists, please check!");
      result.setStatus(1);
      return result;
    }

    // 2) Check target-cluster is already exists
    String clusterId = cluster.getClusterId();
    if (StringUtils.isNotEmpty(clusterId) && this.existsByClusterId(clusterId, cluster.getId())) {
      result.setMsg("The clusterId " + clusterId + " is already exists,please check!");
      result.setStatus(2);
      return result;
    }

    // 3) Check connection
    if (ExecutionMode.isRemoteMode(cluster.getExecutionModeEnum())
        && !cluster.verifyClusterConnection()) {
      result.setMsg("The remote cluster connection failed, please check!");
      result.setStatus(3);
      return result;
    }
    if (ExecutionMode.isYarnMode(cluster.getExecutionModeEnum())
        && cluster.getClusterId() != null
        && !cluster.verifyClusterConnection()) {
      result.setMsg("The flink cluster connection failed, please check!");
      result.setStatus(4);
      return result;
    }

    return result;
  }

  @Override
  public Boolean create(FlinkCluster flinkCluster) {
    flinkCluster.setUserId(commonService.getUserId());
    boolean successful = validateQueueIfNeeded(flinkCluster);
    ApiAlertException.throwIfFalse(
        successful, String.format(ERROR_CLUSTER_QUEUE_HINT, flinkCluster.getYarnQueue()));
    flinkCluster.setCreateTime(new Date());
    if (ExecutionMode.isRemoteMode(flinkCluster.getExecutionModeEnum())) {
      flinkCluster.setClusterState(ClusterState.RUNNING.getValue());
      flinkCluster.setStartTime(new Date());
      flinkCluster.setEndTime(null);
    } else {
      flinkCluster.setClusterState(ClusterState.CREATED.getValue());
    }
    boolean ret = save(flinkCluster);
    if (ret && ExecutionMode.isRemoteMode(flinkCluster.getExecutionMode())) {
      FlinkClusterWatcher.addWatching(flinkCluster);
    }
    return ret;
  }

  @Override
  @Transactional(rollbackFor = {Exception.class})
  public void start(FlinkCluster cluster) {
    FlinkCluster flinkCluster = getById(cluster.getId());
    updateClusterState(cluster.getId(), ClusterState.STARTING);
    try {
      DeployResponse deployResponse = deployInternal(flinkCluster);
      ApiAlertException.throwIfNull(
          deployResponse,
          "Deploy cluster failed, unknown reason，please check you params or StreamPark error log");
      if (ExecutionMode.isYarnSessionMode(flinkCluster.getExecutionModeEnum())) {
        String address =
            String.format(
                "%s/proxy/%s/", YarnUtils.getRMWebAppURL(true), deployResponse.clusterId());
        flinkCluster.setAddress(address);
        flinkCluster.setJobManagerUrl(deployResponse.address());
      } else {
        flinkCluster.setAddress(deployResponse.address());
      }
      flinkCluster.setClusterId(deployResponse.clusterId());
      flinkCluster.setClusterState(ClusterState.RUNNING.getValue());
      flinkCluster.setException(null);
      flinkCluster.setEndTime(null);
      updateById(flinkCluster);
      FlinkClusterWatcher.addWatching(flinkCluster);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      flinkCluster.setClusterState(ClusterState.FAILED.getValue());
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
        success, String.format(ERROR_CLUSTER_QUEUE_HINT, paramOfCluster.getYarnQueue()));

    flinkCluster.setClusterName(paramOfCluster.getClusterName());
    flinkCluster.setDescription(paramOfCluster.getDescription());
    if (ExecutionMode.isRemoteMode(flinkCluster.getExecutionModeEnum())) {
      flinkCluster.setAddress(paramOfCluster.getAddress());
      flinkCluster.setClusterState(ClusterState.RUNNING.getValue());
      flinkCluster.setStartTime(new Date());
      flinkCluster.setEndTime(null);
      FlinkClusterWatcher.addWatching(flinkCluster);
    } else {
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
    updateById(flinkCluster);
  }

  @Override
  public void shutdown(FlinkCluster cluster) {
    FlinkCluster flinkCluster = this.getById(cluster.getId());
    // 1) check mode
    String clusterId = flinkCluster.getClusterId();
    ApiAlertException.throwIfTrue(
        StringUtils.isBlank(clusterId), "The clusterId can not be empty!");

    // 2) check cluster is active
    checkActiveIfNeeded(flinkCluster);

    // 3) check job if running on cluster
    boolean existsRunningJob = applicationService.existsRunningJobByClusterId(flinkCluster.getId());
    ApiAlertException.throwIfTrue(
        existsRunningJob, "Some app is running on this cluster, the cluster cannot be shutdown");

    updateClusterState(flinkCluster.getId(), ClusterState.CANCELING);
    try {
      // 4) shutdown
      ShutDownResponse shutDownResponse = shutdownInternal(flinkCluster, clusterId);
      ApiAlertException.throwIfNull(shutDownResponse, "Get shutdown response failed");
      flinkCluster.setClusterState(ClusterState.CANCELED.getValue());
      flinkCluster.setEndTime(new Date());
      updateById(flinkCluster);
      FlinkClusterWatcher.unWatching(flinkCluster);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      flinkCluster.setException(e.toString());
      updateById(flinkCluster);
      throw new ApiDetailException(
          "Shutdown cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
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
  public Boolean existsByFlinkEnvId(Long flinkEnvId) {
    LambdaQueryWrapper<FlinkCluster> lambdaQueryWrapper =
        new LambdaQueryWrapper<FlinkCluster>().eq(FlinkCluster::getVersionId, flinkEnvId);
    return getBaseMapper().exists(lambdaQueryWrapper);
  }

  @Override
  public List<FlinkCluster> getByExecutionModes(Collection<ExecutionMode> executionModes) {
    return getBaseMapper()
        .selectList(
            new LambdaQueryWrapper<FlinkCluster>()
                .in(
                    FlinkCluster::getExecutionMode,
                    executionModes.stream()
                        .map(ExecutionMode::getMode)
                        .collect(Collectors.toSet())));
  }

  @Override
  public void updateClusterState(Long id, ClusterState state) {
    LambdaUpdateWrapper<FlinkCluster> updateWrapper =
        new LambdaUpdateWrapper<FlinkCluster>()
            .eq(FlinkCluster::getId, id)
            .set(FlinkCluster::getClusterState, state.getValue());

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
  public void delete(FlinkCluster cluster) {
    Long id = cluster.getId();
    FlinkCluster flinkCluster = getById(id);
    ApiAlertException.throwIfNull(flinkCluster, "Flink cluster not exist, please check.");

    if (ExecutionMode.isYarnSessionMode(flinkCluster.getExecutionModeEnum())
        || ExecutionMode.isKubernetesSessionMode(flinkCluster.getExecutionMode())) {
      ApiAlertException.throwIfTrue(
          ClusterState.isRunning(flinkCluster.getClusterStateEnum()),
          "Flink cluster is running, cannot be delete, please check.");
    }

    ApiAlertException.throwIfTrue(
        applicationService.existsJobByClusterId(id),
        "Some app on this cluster, the cluster cannot be delete, please check.");
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
        clusterInfo.getExecutionModeEnum(), clusterInfo.getYarnQueue());
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
    yarnQueueService.checkQueueLabel(newCluster.getExecutionModeEnum(), newCluster.getYarnQueue());
    if (!isYarnNotDefaultQueue(newCluster)) {
      return true;
    }

    if (ExecutionMode.isYarnSessionMode(newCluster.getExecutionModeEnum())
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
   *     default), return true, false else.
   */
  private boolean isYarnNotDefaultQueue(FlinkCluster cluster) {
    return ExecutionMode.isYarnSessionMode(cluster.getExecutionModeEnum())
        && !yarnQueueService.isDefaultQueue(cluster.getYarnQueue());
  }

  private ShutDownResponse shutdownInternal(FlinkCluster flinkCluster, String clusterId)
      throws InterruptedException, ExecutionException, TimeoutException {
    ShutDownRequest stopRequest =
        new ShutDownRequest(
            flinkEnvService.getById(flinkCluster.getVersionId()).getFlinkVersion(),
            flinkCluster.getExecutionModeEnum(),
            flinkCluster.getProperties(),
            clusterId,
            getKubernetesDeployDesc(flinkCluster, "shutdown"));
    Future<ShutDownResponse> future =
        executorService.submit(() -> FlinkClient.shutdown(stopRequest));
    return future.get(60, TimeUnit.SECONDS);
  }

  private DeployResponse deployInternal(FlinkCluster flinkCluster)
      throws InterruptedException, ExecutionException, TimeoutException {
    DeployRequest deployRequest =
        new DeployRequest(
            flinkEnvService.getById(flinkCluster.getVersionId()).getFlinkVersion(),
            flinkCluster.getExecutionModeEnum(),
            flinkCluster.getProperties(),
            flinkCluster.getClusterId(),
            getKubernetesDeployDesc(flinkCluster, "start"));
    log.info("Deploy cluster request " + deployRequest);
    Future<DeployResponse> future = executorService.submit(() -> FlinkClient.deploy(deployRequest));
    return future.get(60, TimeUnit.SECONDS);
  }

  private void checkActiveIfNeeded(FlinkCluster flinkCluster) {
    if (ExecutionMode.isYarnSessionMode(flinkCluster.getExecutionModeEnum())) {
      ApiAlertException.throwIfFalse(
          ClusterState.isRunning(flinkCluster.getClusterStateEnum()),
          "Current cluster is not active, please check!");
      if (!flinkCluster.verifyClusterConnection()) {
        flinkCluster.setClusterState(ClusterState.LOST.getValue());
        updateById(flinkCluster);
        throw new ApiAlertException("Current cluster is not active, please check!");
      }
    }
  }

  @Nullable
  private KubernetesDeployParam getKubernetesDeployDesc(
      @Nonnull FlinkCluster flinkCluster, String action) {
    ExecutionMode executionModeEnum = flinkCluster.getExecutionModeEnum();
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
        throw new ApiAlertException(
            String.format(
                "The ExecutionModeEnum %s can't %s!", executionModeEnum.getName(), action));
    }
    return null;
  }
}
