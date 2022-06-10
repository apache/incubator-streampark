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

package com.streamxhub.streamx.console.core.task;

import static com.streamxhub.streamx.console.core.enums.FlinkAppState.Bridge.toK8sFlinkJobState;

import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.service.AlertService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.flink.kubernetes.FlinkTrkConf;
import com.streamxhub.streamx.flink.kubernetes.K8sFlinkTrkMonitor;
import com.streamxhub.streamx.flink.kubernetes.K8sFlinkTrkMonitorFactory;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode;
import com.streamxhub.streamx.flink.kubernetes.model.TrkId;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

import scala.Enumeration;

/**
 * Flink K8s Tracking Monitor Wrapper.
 * <p>
 * todo Notes
 * Currentlty Tracking Monitor of Flink on K8s and on YARN are independent
 * of each other, this is because the tracking behavior of Flink on K8s is
 * quite difference.
 * Mybe we need to refactor to a unified Flink Tracking Monitor in the
 * future, both tracking-on-k8s and tracking-on-yarn will exist as plugins
 * for this unified implementation.
 * <p>
 * @author Al-assad
 */
@Configuration
public class K8sFlinkTrkMonitorWrapper {

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Lazy
    @Autowired
    private ApplicationService applicationService;

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Lazy
    @Autowired
    private AlertService alertService;

    /**
     * Register FlinkTrkMonitor bean for tracking flink job on kubernetes.
     */
    @Bean(destroyMethod = "close")
    public K8sFlinkTrkMonitor registerFlinkTrackingMonitor() {
        // lazy start tracking monitor
        K8sFlinkTrkMonitor trkMonitor = K8sFlinkTrkMonitorFactory.createInstance(FlinkTrkConf.fromConfigHub(), true);
        initK8sFlinkTrkMonitor(trkMonitor);

        /* Dev scaffold: watch flink k8s tracking cache,
           see com.streamxhub.streamx.flink.kubernetes.helper.TrkMonitorDebugHelper for items.
           Example:
               TrkMonitorDebugHelper.watchTrkIdsCache(trkMonitor);
               TrkMonitorDebugHelper.watchJobStatusCache(trkMonitor);
               TrkMonitorDebugHelper.watchAggClusterMetricsCache(trkMonitor);
               TrkMonitorDebugHelper.watchClusterMetricsCache(trkMonitor);
        */
        return trkMonitor;
    }

    private void initK8sFlinkTrkMonitor(@Nonnull K8sFlinkTrkMonitor trkMonitor) {
        // register change event listener
        trkMonitor.registerListener(new K8sFlinkChangeEventListener(applicationService, alertService));
        // recovery tracking list
        List<TrkId> k8sApp = getK8sTrackingApplicationFromDB();
        k8sApp.forEach(trkMonitor::trackingJob);
    }

    /**
     * get flink-k8s job tracking application from db.
     */
    private List<TrkId> getK8sTrackingApplicationFromDB() {
        // query k8s executiion mode application from db
        final QueryWrapper<Application> queryWrapper = new QueryWrapper<>();
        queryWrapper
            .in("execution_mode", ExecutionMode.getKubernetesMode())
            .eq("tracking", 1);
        List<Application> k8sApplication = applicationService.list(queryWrapper);
        if (CollectionUtils.isEmpty(k8sApplication)) {
            return Lists.newArrayList();
        }
        // correct corrupted data
        List<Application> correctApps = k8sApplication.stream()
            .filter(app -> Bridge.toTrkId(app).nonLegal())
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(correctApps)) {
            applicationService.saveOrUpdateBatch(correctApps);
        }
        // filter out the application that should be tracking
        return k8sApplication.stream()
            .filter(app -> !FlinkJobState.isEndState(toK8sFlinkJobState(app.getFlinkAppStateEnum())))
            .map(Bridge::toTrkId)
            .collect(Collectors.toList());
    }

    /**
     * Type coverter bridge
     */
    public static class Bridge {

        // covert Application to TrkId
        public static TrkId toTrkId(@Nonnull Application app) {
            Enumeration.Value mode = FlinkK8sExecuteMode.of(app.getExecutionModeEnum());
            if (FlinkK8sExecuteMode.APPLICATION().equals(mode)) {
                return TrkId.onApplication(app.getK8sNamespace(), app.getClusterId());
            } else if (FlinkK8sExecuteMode.SESSION().equals(mode)) {
                return TrkId.onSession(app.getK8sNamespace(), app.getClusterId(), app.getJobId());
            } else {
                throw new IllegalArgumentException("Illegal K8sExecuteMode, mode=" + app.getExecutionMode());
            }
        }
    }

    /**
     * Determine if application it is flink-on-kubernetes mode.
     */
    public static boolean isKubernetesApp(Application application) {
        if (application == null) {
            return false;
        }
        return ExecutionMode.isKubernetesMode(application.getExecutionMode());
    }

}
