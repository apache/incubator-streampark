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

package org.apache.streampark.spark.client.impl;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.spark.client.bean.CancelRequest;
import org.apache.streampark.spark.client.bean.CancelResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;
import org.apache.streampark.spark.client.trait.SparkClientTrait;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/** Yarn application mode submit. */
public final class YarnClient extends SparkClientTrait {

    private static final String LOG_PREFIX = "[StreamPark][Spark][YarnClient]";

    public static final YarnClient INSTANCE = new YarnClient();

    private final Map<String, SparkAppHandle> sparkHandles = new ConcurrentHashMap<>();

    private YarnClient() {
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest) throws Exception {
        SparkAppHandle sparkAppHandle = sparkHandles.remove(cancelRequest.appId());
        if (sparkAppHandle != null) {
            try {
                sparkAppHandle.stop();
                logInfo(LOG_PREFIX + " spark job: " + cancelRequest.appId() + " is stopped successfully.");
                return new CancelResponse(null);
            } catch (Exception e) {
                logError(LOG_PREFIX + " sparkAppHandle kill failed. Try kill by yarn", e);
                yarnKill(cancelRequest.appId());
                return new CancelResponse(null);
            }
        }
        logWarn(LOG_PREFIX + " spark job: " + cancelRequest.appId() + " is not existed. Try kill by yarn");
        yarnKill(cancelRequest.appId());
        return new CancelResponse(null);
    }

    private void yarnKill(String appId) throws Exception {
        HadoopUtils.yarnClient().killApplication(ApplicationId.fromString(appId));
        logInfo(LOG_PREFIX + " spark job: " + appId + " is killed by yarn successfully.");
    }

    @Override
    public void setConfig(SubmitRequest submitRequest) {
        // Yarn-specific launcher configuration is applied in setSparkConfig().
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest) throws Exception {
        SparkLauncher launcher = prepareSparkLauncher(submitRequest);
        setSparkConfig(submitRequest, launcher);

        SparkAppHandle handle = launch(launcher);
        if (handle.getError().isPresent()) {
            logInfo(LOG_PREFIX + " spark job: " + submitRequest.appName() + " submit failed.");
            Throwable error = handle.getError().get();
            if (error instanceof Exception) {
                throw (Exception) error;
            }
            throw new IllegalStateException("Spark submit failed", error);
        }
        logInfo(
            LOG_PREFIX
                + " spark job: "
                + submitRequest.appName()
                + " submit successfully, appid: "
                + handle.getAppId()
                + ", state: "
                + handle.getState());
        sparkHandles.put(handle.getAppId(), handle);
        String trackingUrl =
            YarnUtils.getYarnAppTrackingUrl(HadoopUtils.toApplicationId(handle.getAppId()));
        return new SubmitResponse(handle.getAppId(), trackingUrl, submitRequest.appProperties());
    }

    private SparkAppHandle launch(SparkLauncher sparkLauncher) throws Exception {
        logInfo(LOG_PREFIX + " The spark job start submitting");
        CountDownLatch submitFinished = new CountDownLatch(1);
        SparkAppHandle sparkAppHandle =
            sparkLauncher.startApplication(
                new SparkAppHandle.Listener() {

                    @Override
                    public void infoChanged(SparkAppHandle handle) {
                        // Spark launcher info updates are handled in stateChanged().
                    }

                    @Override
                    public void stateChanged(SparkAppHandle handle) {
                        if (handle.getAppId() != null) {
                            logInfo(handle.getAppId() + " stateChanged : " + handle.getState());
                        } else {
                            logInfo("stateChanged : " + handle.getState());
                        }
                        if (handle.getAppId() != null && submitFinished.getCount() != 0) {
                            submitFinished.countDown();
                        }
                        if (handle.getState().isFinal()) {
                            if (StringUtils.isNotBlank(handle.getAppId())
                                && sparkHandles.containsKey(handle.getAppId())) {
                                sparkHandles.remove(handle.getAppId());
                            }
                            if (submitFinished.getCount() != 0) {
                                submitFinished.countDown();
                            }
                            logInfo("Task is end, final state : " + handle.getState());
                        }
                    }
                });
        submitFinished.await();
        return sparkAppHandle;
    }

    private SparkLauncher prepareSparkLauncher(SubmitRequest submitRequest) {
        Map<String, String> env = new HashMap<>();
        if (StringUtils.isNotBlank(submitRequest.hadoopUser())) {
            env.put("HADOOP_USER_NAME", submitRequest.hadoopUser());
        }
        String deployMode;
        if (submitRequest.deployMode() == SparkDeployMode.YARN_CLIENT) {
            deployMode = "client";
        } else if (submitRequest.deployMode() == SparkDeployMode.YARN_CLUSTER) {
            deployMode = "cluster";
        } else {
            throw new IllegalArgumentException(
                LOG_PREFIX + " Invalid spark on yarn deployMode,"
                    + " only support \"client\" and \"cluster\".");
        }
        return new SparkLauncher(env)
            .setSparkHome(submitRequest.sparkVersion().getSparkHome())
            .setAppResource(submitRequest.userJarPath())
            .setMainClass(submitRequest.appMain())
            .setAppName(submitRequest.appName())
            .setConf("spark.yarn.dist.jars", submitRequest.hdfsWorkspace().sparkLib())
            .setConf("spark.yarn.applicationType", "StreamPark Spark")
            .setVerbose(true)
            .setMaster("yarn")
            .setDeployMode(deployMode);
    }

    private void setSparkConfig(SubmitRequest submitRequest, SparkLauncher sparkLauncher) {
        logInfo(LOG_PREFIX + " set spark configuration.");
        if (SparkDeployMode.isYarnMode(submitRequest.deployMode())) {
            setYarnQueue(submitRequest);
        }

        for (Map.Entry<String, String> prop : submitRequest.appProperties().entrySet()) {
            logInfo("| " + prop.getKey() + "  : " + prop.getValue());
            sparkLauncher.setConf(prop.getKey(), prop.getValue());
        }

        for (String arg : submitRequest.appArgs()) {
            sparkLauncher.addAppArgs(arg);
        }
        if (submitRequest.hasExtra("sql")) {
            sparkLauncher.addAppArgs("--sql", submitRequest.getExtra("sql").toString());
        }
    }

    private void setYarnQueue(SubmitRequest submitRequest) {
        if (submitRequest.hasExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_NAME())) {
            submitRequest
                .appProperties()
                .put(
                    ConfigKeys.KEY_SPARK_YARN_QUEUE(),
                    (String) submitRequest.getExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_NAME()));
        }
        if (submitRequest.hasExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_LABEL())) {
            String label = (String) submitRequest.getExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_LABEL());
            submitRequest.appProperties().put(ConfigKeys.KEY_SPARK_YARN_AM_NODE_LABEL(), label);
            submitRequest.appProperties().put(ConfigKeys.KEY_SPARK_YARN_EXECUTOR_NODE_LABEL(), label);
        }
    }
}
