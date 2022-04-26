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

public enum AccessTokenState implements Serializable {

    /**
     * 用户未添加token
     */
    NULL(0),

    /**
     * 用户的token被锁定或者无效
     */
    INVALID(1),

    /**
     * 有效
     */
    OK(2);

    private final int value;

    AccessTokenState(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }

}
