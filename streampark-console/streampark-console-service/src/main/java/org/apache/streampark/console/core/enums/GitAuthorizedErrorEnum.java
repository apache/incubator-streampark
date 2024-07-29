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

/** Git authentication results */
public enum GitAuthorizedErrorEnum {

    /** Success. */
    SUCCESS(0),

    /** Need required, user password is null. */
    REQUIRED(1),

    /** User password error. */
    ERROR(2),

    /** Unknown error. */
    UNKNOW(3);

    private final int value;

    GitAuthorizedErrorEnum(int value) {
        this.value = value;
    }

    public static GitAuthorizedErrorEnum of(Integer state) {
        for (GitAuthorizedErrorEnum error : values()) {
            if (error.value == state) {
                return error;
            }
        }
        return null;
    }

    public int getType() {
        return value;
    }
}
