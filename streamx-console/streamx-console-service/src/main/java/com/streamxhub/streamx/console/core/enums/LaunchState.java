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
public enum LaunchState implements Serializable {

    /**
     * 部署失败
     */
    FAILED(-1),
    /**
     * 完结
     */
    DONE(0),

    /**
     * 任务修改完毕需要重新发布
     */
    NEED_LAUNCH(1),

    /**
     * 上线中
     */
    LAUNCHING(2),

    /**
     * 上线完毕,需要重启
     */
    NEED_RESTART(3),

    //需要回滚
    NEED_ROLLBACK(4),

    /**
     * 项目发生变化,任务需检查(是否需要重新选择jar)
     */
    NEED_CHECK(5),

    /**
     * 发布的任务已经撤销
     */
    REVOKED(10);

    private final int value;

    LaunchState(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }

    public static LaunchState of(Integer state) {
        return Arrays.stream(values()).filter((x) -> x.value == state).findFirst().orElse(null);
    }
}
