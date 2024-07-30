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

import org.apache.streampark.console.base.spi.Status;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SparkMessageStatus implements Status {

    SPARK_ENV_HOME_NULL_ERROR(10710, "The spark home does not exist, please check.",
        "Spark Home不存在，请查验。"),
    SPARK_ENV_HOME_IS_DEFAULT_SET(10720, "The spark home is set as default, please change it first.",
        "Spark Home 设置为默认设置，请先更改"),
    SPARK_ENV_VERSION_NOT_FOUND(10730, "[StreamPark] can no found spark {0} version",
        "[StreamPark] 无法找到Spark {0} 版本"),

    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;
}
