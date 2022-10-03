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

import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.SettingService;
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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    private SettingService settingService;

    @Override
    public String check(FlinkCluster cluster) {
        if (null == cluster.getClusterName() || null == cluster.getExecutionMode()) {
            return "error";
        }
        //1) 检查名称是否重复,是否已经存在
        FlinkCluster flinkCluster = this.baseMapper.getByName(cluster.getClusterName());
        if (flinkCluster != null) {
            if (cluster.getId() == null || (cluster.getId() != null && !flinkCluster.getId().equals(cluster.getId()))) {
                return "exists";
            }
        }
        if (ExecutionMode.REMOTE.equals(cluster.getExecutionModeEnum())) {
            //2) 检查连接是否能连接到
            return cluster.verifyConnection() ? "success" : "fail";
        }
        return "success";
    }

    @Override
    public ResponseResult create(FlinkCluster flinkCluster) {
        ResponseResult result = new ResponseResult();
        if (StringUtils.isBlank(flinkCluster.getClusterName())) {
            result.setMsg("clusterName can't empty!");
            result.setStatus(0);
            return result;
        }
        String clusterId = flinkCluster.getClusterId();
        if (StringUtils.isNoneBlank(clusterId)) {
            FlinkCluster inDB = this.baseMapper.getByClusterId(clusterId);
            if (inDB != null) {
                result.setMsg("the clusterId" + clusterId + "is already exists,please check!");
                result.setStatus(0);
                return result;
            }
        }
        flinkCluster.setUserId(commonService.getCurrentUser().getUserId());
        flinkCluster.setCreateTime(new Date());
        // remote mode directly set STARTED
        if (ExecutionMode.REMOTE.equals(flinkCluster.getExecutionModeEnum())) {
            flinkCluster.setClusterState(ClusterState.STARTED.getValue());
        } else {
            flinkCluster.setClusterState(ClusterState.CREATED.getValue());
        }
        try {
            save(flinkCluster);
            result.setStatus(1);
        } catch (Exception e) {
            result.setStatus(0);
            result.setMsg("create cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public ResponseResult start(FlinkCluster flinkCluster) {
        ResponseResult result = new ResponseResult();
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
                    result.setMsg("the ExecutionModeEnum " + executionModeEnum.getName() + "can't start!");
                    result.setStatus(0);
                    return result;
            }
            FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());
            Map<String, Object> extraParameter = flinkCluster.getOptionMap();
            ResolveOrder resolveOrder = ResolveOrder.of(flinkCluster.getResolveOrder());
            Map<String, String> dynamicOption = FlinkSubmitter.extractDynamicOptionAsJava(flinkCluster.getDynamicOptions());
            DeployRequest deployRequest = new DeployRequest(
                flinkEnv.getFlinkVersion(),
                flinkCluster.getClusterId(),
                executionModeEnum,
                resolveOrder,
                flinkCluster.getFlameGraph() ? getFlameGraph(flinkCluster) : null,
                dynamicOption,
                kubernetesDeployParam,
                extraParameter
            );
            log.info("deploy cluster request " + deployRequest);
            Future<DeployResponse> future = executorService.submit(() -> FlinkSubmitter.deploy(deployRequest));
            DeployResponse deployResponse = future.get(60, TimeUnit.SECONDS);
            if (null != deployResponse) {
                if (deployResponse.message() == null) {
                    updateWrapper.set(FlinkCluster::getClusterId, deployResponse.clusterId());
                    updateWrapper.set(FlinkCluster::getAddress, deployResponse.address());
                    updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STARTED.getValue());
                    updateWrapper.set(FlinkCluster::getException, null);
                    update(updateWrapper);
                    result.setStatus(1);
                    FlinkTrackingTask.removeFlinkCluster(flinkCluster);
                } else {
                    result.setStatus(0);
                    result.setMsg("deploy cluster failed," + deployResponse.message());
                }
            } else {
                result.setStatus(0);
                result.setMsg("deploy cluster failed, unknown reason，please check you params or StreamPark error log");
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STOPED.getValue());
            updateWrapper.set(FlinkCluster::getException, e.toString());
            update(updateWrapper);
            result.setStatus(0);
            result.setMsg("deploy cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
            return result;
        }
    }

    @Override
    public ResponseResult update(FlinkCluster flinkCluster) {
        ResponseResult result = new ResponseResult();
        try {
            updateById(flinkCluster);
            result.setStatus(1);
        } catch (Exception e) {
            result.setStatus(0);
            result.setMsg("update cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
        }
        return result;
    }

    @Override
    public ResponseResult shutdown(FlinkCluster flinkCluster) {
        ResponseResult result = new ResponseResult();
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
                result.setMsg("the ExecutionModeEnum " + executionModeEnum.getName() + "can't shutdown!");
                result.setStatus(0);
                return result;
        }
        if (StringUtils.isBlank(clusterId)) {
            result.setMsg("the clusterId is Empty!");
            result.setStatus(0);
            return result;
        }
        FlinkEnv flinkEnv = flinkEnvService.getById(flinkCluster.getVersionId());
        Map<String, Object> extraParameter = flinkCluster.getOptionMap();
        ShutDownRequest stopRequest = new ShutDownRequest(
            flinkEnv.getFlinkVersion(),
            executionModeEnum,
            clusterId,
            kubernetesDeployParam,
            extraParameter
        );
        LambdaUpdateWrapper<FlinkCluster> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(FlinkCluster::getId, flinkCluster.getId());
        try {
            Future<ShutDownResponse> future = executorService.submit(() -> FlinkSubmitter.shutdown(stopRequest));
            ShutDownResponse shutDownResponse = future.get(60, TimeUnit.SECONDS);
            if (null != shutDownResponse) {
                updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STOPED.getValue());
                update(updateWrapper);
                result.setStatus(1);
                return result;
            }
            result.setStatus(1);
            result.setMsg("clusterId is not exists!");
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            updateWrapper.set(FlinkCluster::getException, e.toString());
            update(updateWrapper);
            result.setStatus(0);
            result.setMsg("shutdown cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
            return result;
        }
    }

    @Override
    public ResponseResult delete(FlinkCluster flinkCluster) {
        ResponseResult result = new ResponseResult();
        if (StringUtils.isNoneBlank(flinkCluster.getClusterId())
            && ClusterState.STARTED.equals(flinkCluster.getClusterStateEnum())
            && !ExecutionMode.REMOTE.equals(flinkCluster.getExecutionModeEnum())) {
            result = shutdown(flinkCluster);
            if (0 == result.getStatus()) {
                return result;
            }
        }
        try {
            removeById(flinkCluster.getId());
            result.setStatus(1);
        } catch (Exception e) {
            result.setStatus(0);
            result.setMsg("delete cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
        }
        return result;
    }

    private Map<String, Serializable> getFlameGraph(FlinkCluster flinkCluster) {
        Map<String, Serializable> flameGraph = new HashMap<>(8);
        flameGraph.put("reporter", "org.apache.streampark.plugin.profiling.reporter.HttpReporter");
        flameGraph.put("type", ApplicationType.STREAMPARK_FLINK.getType());
        flameGraph.put("id", flinkCluster.getId());
        flameGraph.put("url", settingService.getStreamParkAddress().concat("/metrics/report"));
        flameGraph.put("token", Utils.uuid());
        flameGraph.put("sampleInterval", 1000 * 60 * 2);
        flameGraph.put("metricInterval", 1000 * 60 * 2);
        return flameGraph;
    }
}
