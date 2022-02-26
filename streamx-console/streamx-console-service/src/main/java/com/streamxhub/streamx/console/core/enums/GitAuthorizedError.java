/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.enums;

import java.io.Serializable;

/**
 * @author benjobs
 */
public enum GitAuthorizedError implements Serializable {

    /**
     * 没有错误
     */
    SUCCESS(0),

    /**
     * 需要验证,但是用户密码为null
     */
    REQUIRED(1),

    /**
     * 用户名密码错误
     */
    ERROR(2),


    /**
     * 其他未知错误
     */
    UNKNOW(3);

    private final int value;

    GitAuthorizedError(int value) {
        this.value = value;
    }

    public static GitAuthorizedError of(Integer state) {
        for (GitAuthorizedError error : values()) {
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
