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
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.enums.ChangeTypeEnum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@TableName("t_flink_sql")
public class FlinkSql {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    @TableField("`sql`")
    private String sql;

    private String teamResource;
    private String dependency;
    private Integer version = 1;

    /**
     * candidate number: 0: none candidate <br>
     * 1: newly added record becomes a candidate <br>
     * 2: specific history becomes a candidate <br>
     */
    private Integer candidate;

    private Date createTime;
    private transient boolean effective = false;
    /** sql diff */
    private transient boolean sqlDifference = false;
    /** dependency diff */
    private transient boolean dependencyDifference = false;

    private transient Long teamId;

    public FlinkSql() {
    }

    public FlinkSql(FlinkApplication application) {
        this.appId = application.getId();
        this.sql = application.getFlinkSql();
        this.teamResource = application.getTeamResource();
        this.dependency = application.getDependency();
        this.createTime = new Date();
    }

    public FlinkSql(SparkApplication application) {
        this.appId = application.getId();
        this.sql = application.getSparkSql();
        this.teamResource = application.getTeamResource();
        this.dependency = application.getDependency();
        this.createTime = new Date();
    }

    public void decode() {
        this.setSql(DeflaterUtils.unzipString(this.sql));
    }

    public void setToApplication(FlinkApplication application) {
        String encode = Base64.getEncoder().encodeToString(this.sql.getBytes());
        application.setFlinkSql(encode);
        application.setDependency(this.dependency);
        application.setTeamResource(this.teamResource);
        application.setSqlId(this.id);
    }

    public void setToApplication(SparkApplication application) {
        String encode = Base64.getEncoder().encodeToString(this.sql.getBytes());
        application.setSparkSql(encode);
        application.setDependency(this.dependency);
        application.setTeamResource(this.teamResource);
        application.setSqlId(this.id);
    }

    public ChangeTypeEnum checkChange(FlinkSql target) {
        // 1) determine if sql statement has changed
        boolean sqlDifference = !this.getSql().trim().equals(target.getSql().trim());
        // 2) determine if dependency has changed
        Dependency thisDependency = Dependency.toDependency(this.getDependency());
        Dependency targetDependency = Dependency.toDependency(target.getDependency());
        boolean depDifference = !thisDependency.equals(targetDependency);
        // 3) determine if team resource has changed
        boolean teamResDifference = !Objects.equals(this.teamResource, target.getTeamResource());

        if (sqlDifference && depDifference && teamResDifference) {
            return ChangeTypeEnum.ALL;
        }
        if (sqlDifference) {
            return ChangeTypeEnum.SQL;
        }
        if (depDifference) {
            return ChangeTypeEnum.DEPENDENCY;
        }
        if (teamResDifference) {
            return ChangeTypeEnum.TEAM_RESOURCE;
        }
        return ChangeTypeEnum.NONE;
    }

    public void base64Encode() {
        this.sql = Base64.getEncoder().encodeToString(DeflaterUtils.unzipString(this.sql).getBytes());
    }
}
