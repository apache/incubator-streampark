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

import org.apache.streampark.common.conf.Workspace;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@TableName("t_spark_app_backup")
@Slf4j
public class SparkApplicationBackup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    private Long configId;

    private Long sqlId;

    private String path;

    private String description;
    /** version number at the backup */
    private Integer version;

    private Date createTime;

    private transient boolean backup;

    public SparkApplicationBackup() {
    }

    public SparkApplicationBackup(SparkApplication application) {
        this.appId = application.getId();
        this.sqlId = application.getSqlId();
        this.configId = application.getConfigId();
        this.description = application.getBackUpDescription();
        this.createTime = new Date();
        renderPath(application);
    }

    private void renderPath(SparkApplication application) {
        switch (application.getDeployModeEnum()) {
            case LOCAL:
                this.path = String.format(
                    "%s/%d/%d",
                    Workspace.local().APP_BACKUPS(), application.getId(), createTime.getTime());
                break;
            case YARN_CLUSTER:
            case YARN_CLIENT:
                this.path = String.format(
                    "%s/%d/%d",
                    Workspace.remote().APP_BACKUPS(), application.getId(), createTime.getTime());
                break;
            default:
                throw new UnsupportedOperationException(
                    "unsupported deployMode ".concat(application.getDeployModeEnum().getName()));
        }
    }
}
