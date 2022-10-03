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

package org.apache.streampark.common.enums;

import java.io.Serializable;

public enum FlinkSqlValidationFailedType implements Serializable {
    /**
     * Basic test failed (such as null, etc.)
     */
    VERIFY_FAILED(1),
    /**
     * syntax error
     */
    SYNTAX_ERROR(2),
    /**
     * unsupported dialect
     */
    UNSUPPORTED_DIALECT(3),
    /**
     * unsupported sql command
     */
    UNSUPPORTED_SQL(4),
    /**
     * Not at the end of ";"
     */
    ENDS_WITH(5);

    private final int value;

    FlinkSqlValidationFailedType(int value) {
        this.value = value;
    }

    public static FlinkSqlValidationFailedType of(Integer value) {
        for (FlinkSqlValidationFailedType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
