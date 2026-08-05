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

package org.apache.streampark.flink.kubernetes.helper;

import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.flink.kubernetes.DefaultFlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;

import java.util.Timer;
import java.util.TimerTask;

/** Debug helper for FlinkTrackMonitor, only for streampark development, debugging scenarios. */
public final class KubernetesWatcherHelper extends LoggerSupport {

    private static final String CACHE_COUNT_SUFFIX = " => count=";

    private static final KubernetesWatcherHelper INSTANCE = new KubernetesWatcherHelper();

    private KubernetesWatcherHelper() {
    }

    // print job status cache size info
    public static void watchJobStatusCacheSize(FlinkK8sWatcher k8sWatcher) {
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][status-size]-"
                            + System.currentTimeMillis()
                            + " => "
                            + k8sWatcher.getAllJobStatus().size())),
                0,
                1500);
    }

    // print agg flink cluster metrics cache detail
    public static void watchAggClusterMetricsCache(FlinkK8sWatcher k8sWatcher) {
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][agg-metric]-"
                            + System.currentTimeMillis()
                            + " => "
                            + k8sWatcher.getAccGroupMetrics(null))),
                0,
                1500);
    }

    // print all cluster metrics for each flink cluster
    public static void watchClusterMetricsCache(FlinkK8sWatcher k8sWatcher) {
        DefaultFlinkK8sWatcher watcher = (DefaultFlinkK8sWatcher) k8sWatcher;
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][metric]-"
                            + System.currentTimeMillis()
                            + CACHE_COUNT_SUFFIX
                            + watcher.watchController.flinkMetrics.asMap().size()
                            + " | "
                            + watcher.watchController.flinkMetrics.asMap())),
                0,
                1500);
    }

    // print job cache detail
    public static void watchJobStatusCache(FlinkK8sWatcher k8sWatcher) {
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][status]-"
                            + System.currentTimeMillis()
                            + CACHE_COUNT_SUFFIX
                            + k8sWatcher.getAllJobStatus().size()
                            + " | "
                            + k8sWatcher.getAllJobStatus())),
                0,
                1500);
    }

    // print trackId cache detail
    public static void watchTrackIdsCache(FlinkK8sWatcher k8sWatcher) {
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][trackIds]-"
                            + System.currentTimeMillis()
                            + " => "
                            + k8sWatcher.getAllWatchingIds())),
                0,
                1500);
    }

    public static void watchTrackIdsCacheSize(FlinkK8sWatcher k8sWatcher) {
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][trackIds-size]-"
                            + System.currentTimeMillis()
                            + " => "
                            + k8sWatcher.getAllWatchingIds().size())),
                0,
                1500);
    }

    public static void watchK8sEventCache(FlinkK8sWatcher k8sWatcher) {
        DefaultFlinkK8sWatcher watcher = (DefaultFlinkK8sWatcher) k8sWatcher;
        new Timer()
            .scheduleAtFixedRate(
                timerTask(
                    () -> INSTANCE.logInfo(
                        "[flink-k8s][k8s-event]-"
                            + System.currentTimeMillis()
                            + CACHE_COUNT_SUFFIX
                            + watcher.watchController.k8sDeploymentEvents.asMap().size()
                            + " | "
                            + watcher.watchController.k8sDeploymentEvents.asMap())),
                0,
                1500);
    }

    private static TimerTask timerTask(Runnable runnable) {
        return new TimerTask() {

            @Override
            public void run() {
                runnable.run();
            }
        };
    }
}
