/*
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
package com.streamxhub.streamx.console.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

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
    @JsonIgnore
    private Integer version = 1;

    /**
     * 记录要设置的目标要生效的配置
     */
    private Boolean latest;

    @JsonIgnore
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
        this.sql = application.getFlinkSQL();
        this.dependency = application.getDependency();
        this.createTime = new Date();
    }

    public void decode() {
        this.setSql(DeflaterUtils.unzipString(this.sql));
    }

    public void setToApplication(Application application) {
        String encode = Base64.getEncoder().encodeToString(this.sql.getBytes());
        application.setFlinkSQL(encode);
        application.setDependency(this.dependency);
        application.setSqlId(this.id);
    }

    public void base64Encode() {
        this.sql = Base64.getEncoder().encodeToString(DeflaterUtils.unzipString(this.sql).getBytes());
    }
}
