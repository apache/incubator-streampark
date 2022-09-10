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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.core.enums.ChangedType;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Base64;
import java.util.Date;

@Data
@TableName("t_flink_sql")
public class FlinkSql {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appId;
    @TableField("`sql`")
    private String sql;
    private String dependency;
    private Integer version = 1;

    /**
     * candidate number:
     * 0: none candidate <br>
     * 1: newly added record becomes a candidate <br>
     * 2: specific history becomes a candidate <br>
     */
    private Integer candidate;

    private Date createTime;
    private transient boolean effective = false;
    /**
     * sql diff
     */
    private transient boolean sqlDifference = false;
    /**
     * dependency diff
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
        // 1) determine if sql statement has changed
        boolean sqlDifference = !this.getSql().trim().equals(target.getSql().trim());
        // 2) determine if dependency has changed
        Application.Dependency thisDependency = Application.Dependency.toDependency(this.getDependency());
        Application.Dependency targetDependency = Application.Dependency.toDependency(target.getDependency());

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
