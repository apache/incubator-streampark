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
import org.apache.streampark.common.util.SparkEnvUtils;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/** Yarn application mode submit. */
public final class YarnClient extends SparkClientTrait {

    public static final YarnClient INSTANCE = new YarnClient();

    private final Map<String, SparkAppHandle> sparkHandles = new ConcurrentHashMap<>();

    private YarnClient() {}

    @Override
    protected CancelResponse doCancel(CancelRequest cancelRequest) throws Exception {
        SparkAppHandle sparkAppHandle = sparkHandles.remove(cancelRequest.getAppId());
        if (sparkAppHandle != null) {
            try {
                sparkAppHandle.stop();
                logger.info(
                        "[StreamPark][Spark][YarnClient] spark job: {} is stopped successfully.",
                        cancelRequest.getAppId());
                return new CancelResponse(null);
            } catch (Exception e) {
                logger.error(
                        "[StreamPark][Spark][YarnClient] sparkAppHandle kill failed. Try kill by yarn",
                        e);
                yarnKill(cancelRequest.getAppId());
                return new CancelResponse(null);
            }
        }
        logger.warn(
                "[StreamPark][Spark][YarnClient] spark job: {} is not existed. Try kill by yarn",
                cancelRequest.getAppId());
        yarnKill(cancelRequest.getAppId());
        return new CancelResponse(null);
    }

    private void yarnKill(String appId) throws Exception {
        try {
            HadoopUtils.yarnClient().killApplication(ApplicationId.fromString(appId));
            logger.info(
                    "[StreamPark][Spark][YarnClient] spark job: {} is killed by yarn successfully.",
                    appId);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    protected void setConfig(SubmitRequest submitRequest) {}

    @Override
    protected SubmitResponse doSubmit(SubmitRequest submitRequest) throws Exception {
        SparkLauncher launcher = prepareSparkLauncher(submitRequest);
        setSparkConfig(submitRequest, launcher);
        SparkAppHandle handle = launch(launcher);
        if (handle.getError().isPresent()) {
            logger.info(
                    "[StreamPark][Spark][YarnClient] spark job: {} submit failed.",
                    submitRequest.getAppName());
            throw new RuntimeException(handle.getError().get());
        }
        logger.info(
                "[StreamPark][Spark][YarnClient] spark job: {} submit successfully, appid: {}, state: {}",
                submitRequest.getAppName(),
                handle.getAppId(),
                handle.getState());
        sparkHandles.put(handle.getAppId(), handle);
        String trackingUrl =
                YarnUtils.getYarnAppTrackingUrl(HadoopUtils.toApplicationId(handle.getAppId()));
        return new SubmitResponse(handle.getAppId(), trackingUrl, submitRequest.getAppProperties());
    }

    private SparkAppHandle launch(SparkLauncher sparkLauncher)
            throws InterruptedException, IOException {
        logger.info("[StreamPark][Spark][YarnClient] The spark job start submitting");
        CountDownLatch submitFinished = new CountDownLatch(1);
        SparkAppHandle sparkAppHandle =
                sparkLauncher.startApplication(
                        new SparkAppHandle.Listener() {
                            @Override
                            public void infoChanged(SparkAppHandle sparkAppHandle) {}

                            @Override
                            public void stateChanged(SparkAppHandle handle) {
                                if (handle.getAppId() != null) {
                                    logger.info(
                                            "{} stateChanged : {}",
                                            handle.getAppId(),
                                            handle.getState().toString());
                                } else {
                                    logger.info("stateChanged : {}", handle.getState().toString());
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
                                    logger.info(
                                            "Task is end, final state : {}",
                                            handle.getState().toString());
                                }
                            }
                        });
        submitFinished.await();
        return sparkAppHandle;
    }

    private SparkLauncher prepareSparkLauncher(SubmitRequest submitRequest) {
        Map<String, String> env = new HashMap<>();
        if (StringUtils.isNotBlank(submitRequest.getHadoopUser())) {
            env.put("HADOOP_USER_NAME", submitRequest.getHadoopUser());
        }
        SparkEnvUtils.resolveJavaHome(
                        submitRequest.getSparkVersion().getSparkHome(),
                        submitRequest.getSparkVersion().getVersion())
                .ifPresent(
                        javaHome -> {
                            env.put("JAVA_HOME", javaHome);
                            logger.info("[StreamPark][Spark][YarnClient] Using JAVA_HOME: {}", javaHome);
                        });
        String deployMode;
        if (submitRequest.getDeployMode() == SparkDeployMode.YARN_CLIENT) {
            deployMode = "client";
        } else if (submitRequest.getDeployMode() == SparkDeployMode.YARN_CLUSTER) {
            deployMode = "cluster";
        } else {
            throw new IllegalArgumentException(
                    "[StreamPark][Spark][YarnClient] Invalid spark on yarn deployMode, only support \"client\" and \"cluster\".");
        }
        try {
            SparkLauncher launcher =
                    new SparkLauncher(env)
                            .setSparkHome(submitRequest.getSparkVersion().getSparkHome())
                            .setAppResource(submitRequest.getUserJarPath())
                            .setMainClass(submitRequest.getAppMain())
                            .setAppName(submitRequest.getAppName())
                            .setConf("spark.yarn.dist.jars", submitRequest.getHdfsWorkspace().getSparkLib())
                            .setConf("spark.yarn.applicationType", "StreamPark Spark")
                            .setVerbose(true)
                            .setMaster("yarn")
                            .setDeployMode(deployMode);
            SparkEnvUtils.resolveJavaHome(
                            submitRequest.getSparkVersion().getSparkHome(),
                            submitRequest.getSparkVersion().getVersion())
                    .ifPresent(launcher::setJavaHome);
            return launcher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSparkConfig(SubmitRequest submitRequest, SparkLauncher sparkLauncher)
            throws Exception {
        logger.info("[StreamPark][Spark][YarnClient] set spark configuration.");
        if (SparkDeployMode.isYarnMode(submitRequest.getDeployMode())) {
            setYarnQueue(submitRequest);
        }
        for (Map.Entry<String, String> prop : submitRequest.getAppProperties().entrySet()) {
            logger.info("| {}  : {}", prop.getKey(), prop.getValue());
            sparkLauncher.setConf(prop.getKey(), prop.getValue());
        }
        if (submitRequest.getAppArgs() != null) {
            for (String arg : submitRequest.getAppArgs()) {
                sparkLauncher.addAppArgs(arg);
            }
        }
        if (submitRequest.hasExtra("sql")) {
            sparkLauncher.addAppArgs("--sql", submitRequest.getExtra("sql").toString());
        }
    }

    private void setYarnQueue(SubmitRequest submitRequest) {
        if (submitRequest.hasExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_NAME())) {
            submitRequest
                    .getAppProperties()
                    .put(
                            ConfigKeys.KEY_SPARK_YARN_QUEUE(),
                            submitRequest.getExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_NAME()).toString());
        }
        if (submitRequest.hasExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_LABEL())) {
            String label = submitRequest.getExtra(ConfigKeys.KEY_SPARK_YARN_QUEUE_LABEL()).toString();
            submitRequest.getAppProperties().put(ConfigKeys.KEY_SPARK_YARN_AM_NODE_LABEL(), label);
            submitRequest.getAppProperties().put(ConfigKeys.KEY_SPARK_YARN_EXECUTOR_NODE_LABEL(), label);
        }
    }
}
