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

package org.apache.streampark.spark.client.trait;

import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.spark.client.bean.CancelRequest;
import org.apache.streampark.spark.client.bean.CancelResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;

import java.util.HashMap;
import java.util.Map;

/** Base trait for Spark client implementations. */
public abstract class SparkClientTrait extends LoggerSupport {

    public SubmitResponse submit(SubmitRequest submitRequest) throws Exception {
        logInfo(
            String.format(
                "%n--------------------------------------- spark job start"
                    + " -----------------------------------%n"
                    + "    userSparkHome    : %s%n"
                    + "    sparkVersion     : %s%n"
                    + "    appName          : %s%n"
                    + "    jobType          : %s%n"
                    + "    deployMode       : %s%n"
                    + "    applicationType  : %s%n"
                    + "    appArgs          : %s%n"
                    + "    appConf          : %s%n"
                    + "    properties       : %s%n"
                    + "-------------------------------------------------------------------------------------------%n",
                submitRequest.sparkVersion().getSparkHome(),
                submitRequest.sparkVersion().version(),
                submitRequest.appName(),
                submitRequest.jobType().name(),
                submitRequest.deployMode().name(),
                submitRequest.applicationType().getName(),
                submitRequest.appArgs(),
                submitRequest.appConf(),
                String.join(",", submitRequest.appProperties().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new))));

        prepareConfig(submitRequest);
        setConfig(submitRequest);

        try {
            return doSubmit(submitRequest);
        } catch (Exception e) {
            logError(
                "spark job "
                    + submitRequest.appName()
                    + " start failed, deployMode: "
                    + submitRequest.deployMode().getName()
                    + ", detail: "
                    + ExceptionUtils.stringifyException(e));
            throw e;
        }
    }

    public abstract void setConfig(SubmitRequest submitRequest);

    public CancelResponse cancel(CancelRequest stopRequest) throws Exception {
        logInfo(
            String.format(
                "%n----------------------------------------- spark job cancel"
                    + " ----------------------------------%n"
                    + "     userSparkHome     : %s%n"
                    + "     sparkVersion      : %s%n"
                    + "     appId             : %s%n"
                    + "-------------------------------------------------------------------------------------------%n",
                stopRequest.sparkVersion().getSparkHome(),
                stopRequest.sparkVersion().version(),
                stopRequest.appId()));
        return doCancel(stopRequest);
    }

    public abstract SubmitResponse doSubmit(SubmitRequest submitRequest) throws Exception;

    public abstract CancelResponse doCancel(CancelRequest cancelRequest) throws Exception;

    private void prepareConfig(SubmitRequest submitRequest) {
        Map<String, String> userConfig = new HashMap<>();
        for (Map.Entry<String, String> entry : submitRequest.appProperties().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("spark.")) {
                userConfig.put(key, entry.getValue());
            } else {
                logWarn("[StreamPark] config " + key + " doesn't start with \"spark.\" Skip it.");
            }
        }

        Map<String, String> defaultConfig = new HashMap<>();
        for (Map.Entry<String, String> entry : SubmitRequest.DEFAULT_SUBMIT_PARAM.entrySet()) {
            if (!userConfig.containsKey(entry.getKey())
                && !submitRequest.sparkParameterMap().containsKey(entry.getKey())) {
                defaultConfig.put(entry.getKey(), entry.getValue());
            }
        }

        submitRequest.appProperties().clear();
        submitRequest.appProperties().putAll(defaultConfig);
        submitRequest.appProperties().putAll(submitRequest.sparkParameterMap());
        submitRequest.appProperties().putAll(userConfig);
    }
}
