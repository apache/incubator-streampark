/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.enums;

/**
 * @author benjobs
 */

public enum DeployState {

    /**
     * 不需要重新发布
     */
    NONE(0),

    /**
     * 程序更新需要重新发布
     */
    APP_UPDATED(1),

    /**
     * 配置文件更新需要重新启动
     */
    CONF_UPDATED(2),

    /**
     * 程序发布完,需要重新启动.
     */
    NEED_START(3);

    int value;

    DeployState(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }
}
