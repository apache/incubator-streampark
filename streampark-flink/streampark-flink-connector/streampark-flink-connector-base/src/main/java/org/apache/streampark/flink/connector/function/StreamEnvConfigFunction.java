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

package org.apache.streampark.flink.connector.function;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.Serializable;

@FunctionalInterface
public interface StreamEnvConfigFunction extends Serializable {
  /**
   * When used to initialize StreamExecutionEnvironment, it is used to implement this function and
   * customize the parameters to be set...
   *
   * @param environment: StreamExecutionEnvironment instance
   * @param parameterTool: ParameterTool
   */
  void configuration(StreamExecutionEnvironment environment, ParameterTool parameterTool);
}
