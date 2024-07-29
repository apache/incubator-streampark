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

package org.apache.streampark.console.core.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

import java.util.Arrays;

/** Flink GateWay Type */
@Getter
public enum GatewayTypeEnum {

    /** After flink 1.16 (including 1.16) */
    FLINK_V1(1, "flink-v1"),

    /** After flink 1.17 (including 1.17) */
    FLINK_V2(2, "flink-v2"),

    /** After kyuubi 1.7.0 (including 1.7.0) */
    KYUUBI(10, "kyuubi"),
    ;
    @EnumValue
    private final int value;

    private final String identifier;

    GatewayTypeEnum(int value, String identifier) {
        this.value = value;
        this.identifier = identifier;
    }

    public static GatewayTypeEnum of(int value) {
        return Arrays.stream(values())
            .filter(x -> x.value == value)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown GatewayTypeEnum value: " + value));
    }
}
