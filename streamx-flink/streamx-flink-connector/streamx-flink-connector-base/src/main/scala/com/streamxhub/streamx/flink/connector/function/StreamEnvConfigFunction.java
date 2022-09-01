/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.function;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.Serializable;

/**
 * @author benjobs
 */
@FunctionalInterface
public interface StreamEnvConfigFunction extends Serializable {
    /**
     * 用于初始化StreamExecutionEnvironment的时候,用于可以实现该函数,自定义要设置的参数...
     *
     * @param environment:   StreamExecutionEnvironment instance
     * @param parameterTool: ParameterTool
     */
    void configuration(StreamExecutionEnvironment environment, ParameterTool parameterTool);

}

