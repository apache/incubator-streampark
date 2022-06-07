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

import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.console.core.enums.ChangedType;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Base64;
import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_sql")
public class FlinkSql {
    private Long id;
    private Long appId;
    @TableField("`sql`")
    private String sql;
    private String dependency;
    private Integer version = 1;

    /**
     * 候选版本:
     * 0: 非候选 <br>
     * 1: 新增的记录成为候选版本 <br>
     * 2: 指定历史记录的版本成为候选版本 <br>
     */
    private Integer candidate;

    private Date createTime;
    private transient boolean effective = false;
    /**
     * sql 有差异
     */
    private transient boolean sqlDifference = false;
    /**
     * 依赖 有差异
     */
    private transient boolean dependencyDifference = false;

    public FlinkSql() {
    }

    public FlinkSql(Application application) {
        this.appId = application.getId();
        this.sql = application.getFlinkSql();
        this.dependency = application.getDependency();
        this.createTime = new Date();
    }

    public void decode() {
        this.setSql(DeflaterUtils.unzipString(this.sql));
    }

    public void setToApplication(Application application) {
        String encode = Base64.getEncoder().encodeToString(this.sql.getBytes());
        application.setFlinkSql(encode);
        application.setDependency(this.dependency);
        application.setSqlId(this.id);
    }

    public ChangedType checkChange(FlinkSql target) {
        // 1) 判断sql语句是否发生变化
        boolean sqlDifference = !this.getSql().trim().equals(target.getSql().trim());
        // 2) 判断 依赖是否发生变化
        Application.Dependency thisDependency = Application.Dependency.jsonToDependency(this.getDependency());
        Application.Dependency targetDependency = Application.Dependency.jsonToDependency(target.getDependency());
        boolean depDifference = !thisDependency.eq(targetDependency);
        if (sqlDifference && depDifference) {
            return ChangedType.ALL;
        }
        if (sqlDifference) {
            return ChangedType.SQL;
        }
        if (depDifference) {
            return ChangedType.DEPENDENCY;
        }
        return ChangedType.NONE;
    }

    public void base64Encode() {
        this.sql = Base64.getEncoder().encodeToString(DeflaterUtils.unzipString(this.sql).getBytes());
    }
}
