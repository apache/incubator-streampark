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
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.shaded.org.slf4j.Logger;
import org.apache.streampark.spark.client.bean.CancelRequest;
import org.apache.streampark.spark.client.bean.CancelResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;

import java.util.HashMap;
import java.util.Map;

/** Base trait for Spark client implementations. */
public abstract class SparkClientTrait {

    protected final Logger logger =
            StreamParkLoggerFactory.loggerFactory().getLogger(getClass().getName());

    public SubmitResponse submit(SubmitRequest submitRequest) throws Exception {
        logger.info(
                "--------------------------------------- spark job start -----------------------------------\n"
                        + "    userSparkHome    : {}\n"
                        + "    sparkVersion     : {}\n"
                        + "    appName          : {}\n"
                        + "    jobType          : {}\n"
                        + "    deployMode       : {}\n"
                        + "    applicationType  : {}\n"
                        + "    appArgs          : {}\n"
                        + "    appConf          : {}\n"
                        + "    properties       : {}\n"
                        + "-------------------------------------------------------------------------------------------",
                submitRequest.getSparkVersion().getSparkHome(),
                submitRequest.getSparkVersion().version(),
                submitRequest.getAppName(),
                submitRequest.getJobType().name(),
                submitRequest.getDeployMode().name(),
                submitRequest.getApplicationType().getName(),
                submitRequest.getAppArgs(),
                submitRequest.getAppConf(),
                submitRequest.getAppProperties());
        prepareConfig(submitRequest);
        setConfig(submitRequest);
        try {
            return doSubmit(submitRequest);
        } catch (Exception e) {
            logger.error(
                    "spark job {} start failed, deployMode: {}, detail: {}",
                    submitRequest.getAppName(),
                    submitRequest.getDeployMode().getName(),
                    ExceptionUtils.stringifyException(e));
            throw e;
        }
    }

    public CancelResponse cancel(CancelRequest stopRequest) throws Exception {
        logger.info(
                "----------------------------------------- spark job cancel ----------------------------------\n"
                        + "     userSparkHome     : {}\n"
                        + "     sparkVersion      : {}\n"
                        + "     appId             : {}\n"
                        + "-------------------------------------------------------------------------------------------",
                stopRequest.getSparkVersion().getSparkHome(),
                stopRequest.getSparkVersion().version(),
                stopRequest.getAppId());
        return doCancel(stopRequest);
    }

    protected abstract void setConfig(SubmitRequest submitRequest);

    protected abstract SubmitResponse doSubmit(SubmitRequest submitRequest) throws Exception;

    protected abstract CancelResponse doCancel(CancelRequest cancelRequest) throws Exception;

    private void prepareConfig(SubmitRequest submitRequest) {
        Map<String, String> userConfig = new HashMap<>();
        for (Map.Entry<String, String> entry : submitRequest.getAppProperties().entrySet()) {
            if (entry.getKey().startsWith("spark.")) {
                userConfig.put(entry.getKey(), entry.getValue());
            } else {
                logger.warn("[StreamPark] config {} doesn't start with \"spark.\". Skip it.", entry.getKey());
            }
        }
        Map<String, String> defaultConfig = new HashMap<>();
        for (Map.Entry<String, String> entry : submitRequest.getDefaultSubmitParam().entrySet()) {
            if (!userConfig.containsKey(entry.getKey())
                    && !submitRequest.getSparkParameterMap().containsKey(entry.getKey())) {
                defaultConfig.put(entry.getKey(), entry.getValue());
            }
        }
        submitRequest.getAppProperties().clear();
        submitRequest.getAppProperties().putAll(defaultConfig);
        submitRequest.getAppProperties().putAll(submitRequest.getSparkParameterMap());
        submitRequest.getAppProperties().putAll(userConfig);
    }
}
