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
package com.streamxhub.streamx.common.enums;

/**
 * @author benjobs
 */
public enum SqlErrorType {
    /**
     * 基本检验失败(如为null等)
     */
    VERIFY_FAILED(1),
    /**
     * 语法错误
     */
    SYNTAX_ERROR(2),
    /**
     * 不支持的方言
     */
    UNSUPPORTED_DIALECT(3),
    /**
     * 不支持的sql命令
     */
    UNSUPPORTED_SQL(4),
    /**
     * 非";"结尾
     */
    ENDS_WITH(5);
    public int errorType;

    SqlErrorType(int errorType) {
        this.errorType = errorType;
    }

    public static SqlErrorType of(Integer errorType) {
        for (SqlErrorType type : values()) {
            if (type.errorType == errorType) {
                return type;
            }
        }
        return null;
    }

}
