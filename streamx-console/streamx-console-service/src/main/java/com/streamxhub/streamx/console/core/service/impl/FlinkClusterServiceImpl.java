/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.enums.ApplicationType;
import com.streamxhub.streamx.common.enums.ClusterState;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.ResolveOrder;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.core.dao.FlinkClusterMapper;
import com.streamxhub.streamx.console.core.entity.FlinkCluster;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.entity.ResponseResult;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.core.service.FlinkClusterService;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.SettingService;
import com.streamxhub.streamx.flink.submit.FlinkSubmitter;
import com.streamxhub.streamx.flink.submit.bean.DeployRequest;
import com.streamxhub.streamx.flink.submit.bean.DeployResponse;
import com.streamxhub.streamx.flink.submit.bean.KubernetesDeployParam;
import com.streamxhub.streamx.flink.submit.bean.ShutDownRequest;
import com.streamxhub.streamx.flink.submit.bean.ShutDownResponse;

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

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkClusterServiceImpl extends ServiceImpl<FlinkClusterMapper, FlinkCluster> implements FlinkClusterService {

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2,
        200,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streamx-cluster-executor"),
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
        //clusterId不能重复
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
        //REMOTE模式直接设置为已启动
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
            String[] dynamicOption = CommonUtils.notEmpty(flinkCluster.getDynamicOptions()) ? flinkCluster.getDynamicOptions().split("\\s+") : new String[0];
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
                    updateWrapper.eq(FlinkCluster::getId, flinkCluster.getId());
                    updateWrapper.set(FlinkCluster::getClusterId, deployResponse.clusterId());
                    updateWrapper.set(FlinkCluster::getAddress, deployResponse.address());
                    updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STARTED.getValue());
                    updateWrapper.set(FlinkCluster::getException, null);
                    update(flinkCluster, updateWrapper);
                    result.setStatus(1);
                } else {
                    result.setStatus(0);
                    result.setMsg("deploy cluster failed," + deployResponse.message());
                }
            } else {
                result.setStatus(0);
                result.setMsg("deploy cluster failed, unknown reason，please check you params or streamX error log");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            updateWrapper.eq(FlinkCluster::getId, flinkCluster.getId());
            updateWrapper.set(FlinkCluster::getClusterState, ClusterState.STOPED.getValue());
            updateWrapper.set(FlinkCluster::getException, e.toString());
            update(flinkCluster, updateWrapper);
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
                update(flinkCluster, updateWrapper);
                result.setStatus(1);
                return result;
            }
            result.setStatus(1);
            result.setMsg("clusterId is not exists!");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            updateWrapper.set(FlinkCluster::getException, e.toString());
            update(flinkCluster, updateWrapper);
            result.setStatus(0);
            result.setMsg("shutdown cluster failed, Caused By: " + ExceptionUtils.getStackTrace(e));
            return result;
        }
    }

    @Override
    public ResponseResult delete(FlinkCluster flinkCluster) {
        ResponseResult result = new ResponseResult();
        //clusterId非空 集群为已启动状态 非REMOTE模式
        if (StringUtils.isNoneBlank(flinkCluster.getClusterId()) && ClusterState.STARTED.equals(flinkCluster.getClusterStateEnum()) && !ExecutionMode.REMOTE.equals(flinkCluster.getExecutionModeEnum())) {
            //集群未正常停止，无法删除
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
        flameGraph.put("reporter", "com.streamxhub.streamx.plugin.profiling.reporter.HttpReporter");
        flameGraph.put("type", ApplicationType.STREAMX_FLINK.getType());
        flameGraph.put("id", flinkCluster.getId());
        flameGraph.put("url", settingService.getStreamXAddress().concat("/metrics/report"));
        flameGraph.put("token", Utils.uuid());
        flameGraph.put("sampleInterval", 1000 * 60 * 2);
        flameGraph.put("metricInterval", 1000 * 60 * 2);
        return flameGraph;
    }
}
