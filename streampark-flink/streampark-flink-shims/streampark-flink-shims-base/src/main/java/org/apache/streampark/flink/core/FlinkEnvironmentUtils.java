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

package org.apache.streampark.flink.core;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment;

public final class FlinkEnvironmentUtils {

    private FlinkEnvironmentUtils() {
    }

    public static String getAppName(ParameterTool parameterTool) {
        return getAppName(parameterTool, null, false);
    }

    public static String getAppName(ParameterTool parameterTool, String name, boolean required) {
        String appName = name;
        if (appName == null) {
            try {
                appName =
                    DeflaterUtils.unzipString(parameterTool.get(ConfigKeys.KEY_APP_NAME(), null));
            } catch (Exception ignored) {
                // match Scala Try(...).getOrElse(...)
            }
            if (appName == null) {
                appName = parameterTool.get(ConfigKeys.KEY_FLINK_APP_NAME(), null);
            }
        }
        if (required && appName == null) {
            throw new IllegalArgumentException("[StreamPark] Application name cannot be null");
        }
        return appName;
    }

    public static TableEnvironment setAppName(TableEnvironment env, ParameterTool parameter) {
        String appName = getAppName(parameter);
        if (appName != null) {
            env.getConfig().getConfiguration().setString(PipelineOptions.NAME, appName);
        }
        return env;
    }

    public static StreamTableEnvironment setAppName(
                                                    StreamTableEnvironment env, ParameterTool parameter) {
        String appName = getAppName(parameter);
        if (appName != null) {
            env.getConfig().getConfiguration().setString(PipelineOptions.NAME, appName);
        }
        return env;
    }
}
