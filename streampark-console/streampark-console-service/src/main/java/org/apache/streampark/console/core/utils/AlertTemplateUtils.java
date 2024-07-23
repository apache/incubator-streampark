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

package org.apache.streampark.console.core.utils;

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.core.bean.AlertProbeMsg;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.CheckPointStatusEnum;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.SparkAppStateEnum;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AlertTemplateUtils {

    private static final String ALERT_SUBJECT_PREFIX = "StreamPark Alert:";

    private static final String ALERT_TITLE_PREFIX = "Notify:";

    private static final String PROBE = "PROBE";

    public static AlertTemplate createAlertTemplate(Application application, FlinkAppStateEnum appState) {
        return AlertTemplate.builder()
            .duration(application.getStartTime(), application.getEndTime())
            .jobName(application.getJobName())
            .link(application.getFlinkExecutionMode(), application.getClusterId())
            .startTime(application.getStartTime())
            .endTime(application.getEndTime())
            .restart(application.isNeedRestartOnFailed(), application.getRestartCount())
            .restartIndex(application.getRestartCount())
            .totalRestart(application.getRestartSize())
            .type(1)
            .title(
                String.format(
                    "%s %s %s", ALERT_TITLE_PREFIX, application.getJobName(),
                    appState.name()))
            .subject(
                String.format("%s %s %s", ALERT_SUBJECT_PREFIX, application.getJobName(),
                    appState))
            .status(appState.name())
            .build();
    }

    public static AlertTemplate createAlertTemplate(Application application, CheckPointStatusEnum statusEnum) {
        return AlertTemplate.builder()
            .duration(application.getStartTime(), application.getEndTime())
            .jobName(application.getJobName())
            .link(application.getFlinkExecutionMode(), application.getClusterId())
            .startTime(application.getStartTime())
            .type(2)
            .cpFailureRateInterval(
                DateUtils.toDuration(application.getCpFailureRateInterval() * 1000 * 60))
            .cpMaxFailureInterval(application.getCpMaxFailureInterval())
            .title(
                String.format("%s %s checkpoint FAILED", ALERT_TITLE_PREFIX,
                    application.getJobName()))
            .subject(
                String.format(
                    "%s %s, checkPoint is Failed", ALERT_SUBJECT_PREFIX,
                    application.getJobName()))
            .build();
    }

    public static AlertTemplate createAlertTemplate(FlinkCluster cluster, ClusterState clusterState) {
        return AlertTemplate.builder()
            .duration(cluster.getStartTime(), cluster.getEndTime())
            .jobName(cluster.getClusterName())
            .link(cluster.getFlinkExecutionModeEnum(), cluster.getClusterId())
            .startTime(cluster.getStartTime())
            .endTime(cluster.getEndTime())
            .type(3)
            .title(
                String.format(
                    "%s %s %s", ALERT_TITLE_PREFIX, cluster.getClusterName(),
                    clusterState.name()))
            .subject(
                String.format("%s %s %s", ALERT_SUBJECT_PREFIX, cluster.getClusterName(),
                    clusterState))
            .status(clusterState.name())
            .allJobs(cluster.getAllJobs())
            .affectedJobs(cluster.getAffectedJobs())
            .build();
    }

    public static AlertTemplate createAlertTemplate(AlertProbeMsg alertProbeMsg) {
        return AlertTemplate.builder()
            .type(4)
            .user(alertProbeMsg.getUser())
            .probeJobs(alertProbeMsg.getProbeJobs())
            .failedJobs(alertProbeMsg.getFailedJobs())
            .lostJobs(alertProbeMsg.getLostJobs())
            .cancelledJobs(alertProbeMsg.getCancelledJobs())
            .subject(String.format("%s %s", ALERT_SUBJECT_PREFIX, PROBE))
            .title(PROBE)
            .build();
    }

    public static AlertTemplate createAlertTemplate(SparkApplication application, SparkAppStateEnum appState) {
        return AlertTemplate.builder()
            .duration(application.getStartTime(), application.getEndTime())
            .jobName(application.getJobName())
            .link(application.getSparkExecutionMode(), application.getJobId())
            .startTime(application.getStartTime())
            .endTime(application.getEndTime())
            .restart(application.isNeedRestartOnFailed(), application.getRestartCount())
            .restartIndex(application.getRestartCount())
            .totalRestart(application.getRestartSize())
            .type(1)
            .title(
                String.format(
                    "%s %s %s", ALERT_TITLE_PREFIX, application.getJobName(), appState.name()))
            .subject(
                String.format("%s %s %s", ALERT_SUBJECT_PREFIX, application.getJobName(), appState))
            .status(appState.name())
            .build();
    }
}
