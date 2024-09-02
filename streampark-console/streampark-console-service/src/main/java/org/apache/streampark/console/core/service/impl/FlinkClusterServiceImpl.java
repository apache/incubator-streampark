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
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.YarnQueueService;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.DeployRequest;
import org.apache.streampark.flink.client.bean.DeployResponse;
import org.apache.streampark.flink.client.bean.KubernetesDeployRequest;
import org.apache.streampark.flink.client.bean.ShutDownResponse;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkClusterServiceImpl extends ServiceImpl<FlinkClusterMapper, FlinkCluster>
    implements FlinkClusterService {

  private static final String ERROR_CLUSTER_QUEUE_HINT =
      "Queue label '%s' isn't available in database, please add it first.";

  @Autowired private FlinkEnvService flinkEnvService;

  @Autowired private ServiceHelper serviceHelper;

  @Autowired private ApplicationService applicationService;

  @Autowired private YarnQueueService yarnQueueService;

  @Autowired private SettingService settingService;

  private static final int CPU_NUM = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

  private final ExecutorService bootstrapExecutor =
      new ThreadPoolExecutor(
          1,
          CPU_NUM,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          ThreadUtils.threadFactory("streampark-flink-cluster-bootstrap"));

  @Override
  public ResponseResult check(FlinkCluster cluster) {
    ResponseResult result = new ResponseResult();
    result.setStatus(0);

    // 1) Check name is already exists
    Boolean existsByClusterName =
        this.existsByClusterName(cluster.getClusterName(), cluster.getId());
    if (existsByClusterName) {
      result.setMsg("clusterName is already exists,please check!");
      result.setStatus(1);
      return result;
    }

    // 2) Check target-cluster is already exists
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
    } else if (ExecutionMode.YARN_SESSION.equals(cluster.getExecutionModeEnum())
        && cluster.getClusterId() != null) {
      if (!cluster.verifyClusterConnection()) {
        result.setMsg("the flink cluster connection failed, please check!");
        result.setStatus(4);
        return result;
      }
    }

    return result;
  }

  @Override
  public Boolean create(FlinkCluster flinkCluster, Long userId) {
    flinkCluster.setUserId(userId);
    boolean successful = validateQueueIfNeeded(flinkCluster);
    ApiAlertException.throwIfFalse(
        successful, String.format(ERROR_CLUSTER_QUEUE_HINT, flinkCluster.getYarnQueue()));
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
  public void start(Long id) {
    FlinkCluster flinkCluster = getById(id);
    ApiAlertException.throwIfTrue(flinkCluster == null, "Invalid id, no related cluster found.");
    ExecutionMode executionModeEnum = flinkCluster.getExecutionModeEnum();
    if (executionModeEnum == ExecutionMode.YARN_SESSION) {
      ApiAlertException.throwIfTrue(
          !applicationService.getYARNApplication(flinkCluster.getClusterName()).isEmpty(),
          "The application name: "
              + flinkCluster.getClusterName()
              + " is already running in the yarn queue, please check!");
    }

    try {
      // 1) deployRequest
      DeployRequest deployRequest = getDeployRequest(flinkCluster);

      log.info("deploy cluster request: {}", deployRequest);
      Future<DeployResponse> future =
          bootstrapExecutor.submit(() -> FlinkClient.deploy(deployRequest));
      DeployResponse deployResponse = future.get();
      if (deployResponse.error() != null) {
        throw new ApiDetailException(
            "deploy cluster "
                + flinkCluster.getClusterName()
                + "failed, exception:\n"
                + Utils.stringifyException(deployResponse.error()));
      } else {
        // 2) setAddress
        if (ExecutionMode.YARN_SESSION.equals(executionModeEnum)) {
          String address =
              YarnUtils.getRMWebAppProxyURL() + "/proxy/" + deployResponse.clusterId() + "/";
          flinkCluster.setAddress(address);
        } else {
          flinkCluster.setAddress(deployResponse.address());
        }
        flinkCluster.setClusterId(deployResponse.clusterId());
        flinkCluster.setClusterState(ClusterState.STARTED.getValue());
        flinkCluster.setException(null);
        updateById(flinkCluster);

        // k8s session mode ingress
        if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(executionModeEnum)) {
          try {
            serviceHelper.configureIngress(
                flinkCluster.getClusterId(), flinkCluster.getK8sNamespace());
          } catch (KubernetesClientException e) {
            log.info("Failed to create ingress: {}", e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      flinkCluster.setClusterState(ClusterState.STOPPED.getValue());
      flinkCluster.setException(e.toString());
      updateById(flinkCluster);
      throw new ApiDetailException(e);
    }
  }

  private DeployRequest getDeployRequest(FlinkCluster flinkCluster) {
    ExecutionMode executionModeEnum = flinkCluster.getExecutionModeEnum();
    FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());
    switch (executionModeEnum) {
      case YARN_SESSION:
        return DeployRequest.apply(
            flinkEnv.getFlinkVersion(),
            executionModeEnum,
            flinkCluster.getProperties(),
            flinkCluster.getClusterId(),
            flinkCluster.getClusterName());
      case KUBERNETES_NATIVE_SESSION:
        return KubernetesDeployRequest.apply(
            flinkEnv.getFlinkVersion(),
            executionModeEnum,
            flinkCluster.getProperties(),
            flinkCluster.getClusterId(),
            flinkCluster.getClusterName(),
            flinkCluster.getK8sNamespace(),
            flinkCluster.getK8sConf(),
            flinkCluster.getServiceAccount(),
            flinkCluster.getFlinkImage(),
            flinkCluster.getK8sRestExposedTypeEnum());
      default:
        throw new ApiAlertException(
            "the ExecutionModeEnum " + executionModeEnum.getName() + "can't start!");
    }
  }

  @Override
  public void update(FlinkCluster cluster) {
    FlinkCluster flinkCluster = getById(cluster.getId());
    boolean success = validateQueueIfNeeded(flinkCluster, cluster);
    ApiAlertException.throwIfFalse(
        success, String.format(ERROR_CLUSTER_QUEUE_HINT, cluster.getYarnQueue()));
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
      throw new ApiDetailException(
          "update cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
    }
  }

  @Override
  public void shutdown(Long id) {
    FlinkCluster flinkCluster = this.getById(id);
    // 1) check mode
    ExecutionMode executionModeEnum = flinkCluster.getExecutionModeEnum();
    String clusterId = flinkCluster.getClusterId();
    if (StringUtils.isBlank(clusterId)) {
      throw new ApiAlertException("the clusterId can not be empty!");
    }

    // 2) check cluster is active
    if (ExecutionMode.YARN_SESSION.equals(executionModeEnum)) {
      if (ClusterState.STARTED.equals(ClusterState.of(flinkCluster.getClusterState()))) {
        if (!flinkCluster.verifyClusterConnection()) {
          flinkCluster.setAddress(null);
          flinkCluster.setClusterState(ClusterState.LOST.getValue());
          updateById(flinkCluster);
          throw new ApiAlertException("current cluster is not active, please check");
        }
      } else {
        throw new ApiAlertException("current cluster is not active, please check");
      }
    }

    // 3) check job if running on cluster
    boolean existsRunningJob = applicationService.existsRunningJobByClusterId(flinkCluster.getId());
    if (existsRunningJob) {
      throw new ApiAlertException(
          "There are some jobs running on the cluster, so the cluster cannot be shut down. \uD83D\uDE14\n"
              + "\n");
    }

    // 4) shutdown
    DeployRequest deployRequest = getDeployRequest(flinkCluster);
    try {
      Future<ShutDownResponse> future =
          bootstrapExecutor.submit(() -> FlinkClient.shutdown(deployRequest));
      ShutDownResponse shutDownResponse = future.get();
      if (shutDownResponse.error() != null) {
        throw new ApiDetailException(
            "shutdown cluster failed, error: \n"
                + Utils.stringifyException(shutDownResponse.error()));
      } else {
        flinkCluster.setAddress(null);
        flinkCluster.setClusterState(ClusterState.STOPPED.getValue());
        updateById(flinkCluster);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      flinkCluster.setException(e.toString());
      updateById(flinkCluster);
      throw new ApiDetailException(
          "shutdown cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
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
  public List<FlinkCluster> listCluster() {
    List<FlinkCluster> clusters = list();
    for (FlinkCluster cluster : clusters) {
      if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(cluster.getExecutionModeEnum())) {
        if (StringUtils.isNotBlank(settingService.getIngressModeDefault())) {
          String namespace = cluster.getK8sNamespace();
          String clusterId = cluster.getClusterId();
          String ingressUrl = KubernetesRetriever.getSessionClusterIngressURL(namespace, clusterId);
          if (ingressUrl != null) {
            cluster.setAddress(ingressUrl);
          }
        }
      }
    }
    return clusters;
  }

  @Override
  public IPage<FlinkCluster> findPage(FlinkCluster flinkCluster, RestRequest restRequest) {
    Page<FlinkCluster> page = MybatisPager.getPage(restRequest);
    return this.baseMapper.findPage(page, flinkCluster);
  }

  @Override
  public void delete(Long id) {
    FlinkCluster flinkCluster = getById(id);
    if (flinkCluster == null) {
      throw new ApiAlertException("flink cluster not exist, please check.");
    }

    if (ExecutionMode.YARN_SESSION.equals(flinkCluster.getExecutionModeEnum())
        || ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(flinkCluster.getExecutionModeEnum())) {
      if (ClusterState.STARTED.equals(flinkCluster.getClusterStateEnum())) {
        throw new ApiAlertException("flink cluster is running, cannot be delete, please check.");
      }
    }

    if (applicationService.existsJobByClusterId(id)) {
      throw new ApiAlertException(
          "some app on this cluster, the cluster cannot be delete, please check.");
    }
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
}
