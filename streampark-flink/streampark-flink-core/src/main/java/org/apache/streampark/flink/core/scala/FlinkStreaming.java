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

package org.apache.streampark.flink.core.scala;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.core.FlinkStreamingInitializer;
import org.apache.streampark.flink.core.StreamEnvConfig;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.Serializable;

/** Base class for Flink streaming applications. */
public abstract class FlinkStreaming implements Serializable {

    protected StreamingContext context;

    protected JobExecutionResult jobExecutionResult;

    public final void main(String[] args) {
        init(args);
        ready();
        handle();
        jobExecutionResult = context.start();
        destroy();
    }

    private void init(String[] args) {
        SystemPropertyUtils.setAppHome(ConfigKeys.KEY_APP_HOME(), FlinkStreaming.class);
        context =
            new StreamingContext(
                FlinkStreamingInitializer.initialize(
                    new StreamEnvConfig(
                        args,
                        (environment, parameter) -> config(environment, parameter))));
    }

    protected ParameterTool getParameter() {
        return context.parameter;
    }

    protected void ready() {
    }

    protected void config(StreamExecutionEnvironment environment, ParameterTool parameter) {
    }

    protected abstract void handle();

    protected void destroy() {
    }
}
