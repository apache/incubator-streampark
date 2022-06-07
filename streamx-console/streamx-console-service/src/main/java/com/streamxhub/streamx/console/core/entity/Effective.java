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

package com.streamxhub.streamx.console.core.entity;

import com.streamxhub.streamx.console.core.enums.EffectiveType;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_effective")
@Slf4j
public class Effective {
    private Long id;
    private Long appId;
    /**
     * 1) config <br>
     * 2) flink Sql<br>
     */
    private Integer targetType;
    private Long targetId;
    private Date createTime;

    private transient EffectiveType effectiveType;

    public Effective() {

    }

    public Effective(Long appId, EffectiveType type, Long targetId) {
        this.appId = appId;
        this.targetType = type.getType();
        this.targetId = targetId;
        this.createTime = new Date();
    }

}
