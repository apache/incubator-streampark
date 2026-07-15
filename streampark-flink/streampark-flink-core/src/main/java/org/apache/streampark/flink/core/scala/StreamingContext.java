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

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.core.FlinkParameterUtils;
import org.apache.streampark.flink.core.FlinkStreamingInitializer;
import org.apache.streampark.flink.core.StreamEnvConfig;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/** Flink streaming execution environment context. */
public class StreamingContext {

    public final ParameterTool parameter;

    private final StreamExecutionEnvironment environment;

    public StreamingContext(ParameterTool parameter, StreamExecutionEnvironment environment) {
        this.parameter = parameter;
        this.environment = environment;
    }

    public StreamingContext(FlinkStreamingInitializer.StreamingInitResult init) {
        this(init.parameter, init.streamEnv);
    }

    public StreamingContext(StreamEnvConfig config) {
        this(FlinkStreamingInitializer.initialize(config));
    }

    /** Returns the underlying Java stream execution environment. */
    public StreamExecutionEnvironment getJavaEnv() {
        return environment;
    }

    /** Recommended API to start tasks. */
    public JobExecutionResult start() {
        return execute();
    }

    @Deprecated
    public JobExecutionResult execute() {
        String appName = FlinkParameterUtils.getAppName(parameter, true);
        return execute(appName);
    }

    @Deprecated
    public JobExecutionResult execute(String jobName) {
        Utils.printLogo("FlinkStreaming " + jobName + " Starting...");
        try {
            return environment.execute(jobName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
