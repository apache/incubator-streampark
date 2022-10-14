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

import java.util.Optional;

/**
 * status enum
 */
public enum Status {

    SUCCESS(0, "success"),

    INTERNAL_SERVER_ERROR_ARGS(10000, "Internal Server Error: {0}"),

    USER_CURRENTLY_LOCKED(10002, "user is currently locked"),
    USER_NAME_EXIST(10003, "user name already exists"),
    USER_NAME_NULL(10004, "user name is null"),
    USER_NOT_EXIST(10005, "user {0} not exists"),
    NOT_SUPPORTED_REQUEST_METHOD(10006, "not supported request method exception: {0}"),
    API_FAIL(10007, "API Invoke exception：{0}"),
    REQUEST_PARAMS_NOT_VALID_ERROR(10007, "request parameter {0} is not valid"),
    READ_BUILD_LOG_EXECPTION(10008, "read build log exception: {0}"),
    ACCESSTOKEN_COULD_NOT_FOUND(10009, "accesstoken_could_not_found");
    private final int code;
    private final String enMsg;

    Status(int code, String enMsg) {
        this.code = code;
        this.enMsg = enMsg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.enMsg;
    }

    /**
     * Retrieve Status enum entity by status code.
     */
    public static Optional<Status> findStatusBy(int code) {
        for (Status status : Status.values()) {
            if (code == status.getCode()) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}