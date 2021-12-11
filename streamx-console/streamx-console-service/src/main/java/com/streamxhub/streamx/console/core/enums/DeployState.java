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
import java.util.Arrays;

/**
 * @author benjobs
 */
public enum DeployState implements Serializable {

    /**
     * 需用重新发布,但是下载maven依赖失败.(针对flinkSql任务)
     */
    NEED_DEPLOY_DOWN_DEPENDENCY_FAILED(-1),
    /**
     * 完结
     */
    DONE(0),

    /**
     * 正在部署中
     */
    DEPLOYING(1),

    /**
     * 程序更新需要重新发布
     */
    NEED_DEPLOY_AFTER_BUILD(2),

    /**
     * 依赖更新需要重新发布
     */
    NEED_DEPLOY_AFTER_DEPENDENCY_UPDATE(3),

    /**
     * 配置文件更新需要重新启动
     */
    NEED_RESTART_AFTER_CONF_UPDATE(4),

    /**
     * sql更新需要重新启动
     */
    NEED_RESTART_AFTER_SQL_UPDATE(5),

    /**
     * 发布完成,需要重新启动.
     */
    NEED_RESTART_AFTER_DEPLOY(6),

    //需要回滚
    NEED_ROLLBACK(7),

    /**
     * 回滚完成,需要重新启动
     */
    NEED_RESTART_AFTER_ROLLBACK(8),

    /**
     * 发布的任务已经撤销
     */
    REVOKED(8);

    int value;

    DeployState(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }

    public static DeployState of(Integer state) {
        return Arrays.stream(values()).filter((x) -> x.value == state).findFirst().orElse(null);
    }
}
