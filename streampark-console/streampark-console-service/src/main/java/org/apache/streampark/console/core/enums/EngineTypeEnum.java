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

/** Compute engine type. */
@Getter
public enum EngineTypeEnum {

    /** Apache Flink: activated by default */
    FLINK(1),

    /** Apache Spark */
    SPARK(2);

    @EnumValue
    private final int code;

    EngineTypeEnum(int code) {
        this.code = code;
    }

    public static EngineTypeEnum of(Integer code) {
        return Arrays.stream(values()).filter((x) -> x.code == code).findFirst().orElse(null);
    }
}
