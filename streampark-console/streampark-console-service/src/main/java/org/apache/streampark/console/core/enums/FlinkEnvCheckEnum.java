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

import lombok.Getter;

/* the flink environment status */
@Getter
public enum FlinkEnvCheckEnum {

    /* FLINK_HOME path invalid */
    INVALID_PATH(-1),

    /* this add/update operation ok */
    OK(0),

    /* flink name repeated */
    NAME_REPEATED(1),

    /* FLINK_DIST file not found */

    FLINK_DIST_NOT_FOUND(2),

    /* defined flink name repeated */
    FLINK_DIST_REPEATED(3);

    private final int code;

    FlinkEnvCheckEnum(int code) {
        this.code = code;
    }
}
