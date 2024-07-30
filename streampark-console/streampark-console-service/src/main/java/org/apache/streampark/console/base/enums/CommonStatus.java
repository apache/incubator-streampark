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

package org.apache.streampark.console.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommonStatus implements Status {

    SUCCESS(0, "success", "成功"),
    UNKNOWN_ERROR(1, "unknown error: {0}", "未知错误: {0}"),

    PROJECT(10, "Project", "项目"),
    TEAM(11, "Team", "团队"),
    VARIABLE(12, "Variable", "变量"),
    APPLICATION(13, "Application", "应用程序"),
    FLINK_CLUSTERS(14, "Flink Clusters", "Flink集群"),

    ;

    private final int code;
    private final String enMsg;
    private final String zhMsg;
}
