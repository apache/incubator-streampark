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
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;

import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.util.ParameterTool;

import javax.annotation.Nullable;

/** Parameter and table environment helpers for Flink application naming. */
public final class FlinkParameterUtils {

    private FlinkParameterUtils() {
    }

    public static String getAppName(ParameterTool parameterTool) {
        return getAppName(parameterTool, null, false);
    }

    public static String getAppName(ParameterTool parameterTool, boolean required) {
        return getAppName(parameterTool, null, required);
    }

    public static String getAppName(
                                    ParameterTool parameterTool, @Nullable String name, boolean required) {
        String appName;
        if (name == null) {
            appName = null;
            String zippedAppName = parameterTool.get(ConfigKeys.KEY_APP_NAME(), null);
            if (zippedAppName != null) {
                try {
                    appName = DeflaterUtils.unzipString(zippedAppName);
                } catch (Exception ignored) {
                    // fall back to pipeline.name
                }
            }
            if (appName == null) {
                appName = parameterTool.get(ConfigKeys.KEY_FLINK_APP_NAME(), null);
            }
        } else {
            appName = name;
        }
        if (required) {
            AssertUtils.required(
                appName != null, "[StreamPark] Application name cannot be null");
        }
        return appName;
    }

    public static <T extends TableEnvironment> T setAppName(T env, ParameterTool parameter) {
        String appName = getAppName(parameter);
        if (appName != null) {
            env.getConfig().getConfiguration().setString(PipelineOptions.NAME.key(), appName);
        }
        return env;
    }

    public static StreamTableEnvironment setAppName(
                                                    StreamTableEnvironment env, ParameterTool parameter) {
        String appName = getAppName(parameter);
        if (appName != null) {
            env.getConfig().getConfiguration().setString(PipelineOptions.NAME.key(), appName);
        }
        return env;
    }
}
