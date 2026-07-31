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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** Cache record for an immutable artifact staged into a managed Flink environment. */
@Getter
@Setter
@TableName("t_managed_flink_artifact")
public class ManagedFlinkArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long managedEnvId;

    private Long sourceResourceId;

    private String checksum;

    private String fileName;

    private Long fileSize;

    private String providerArtifactId;

    private Integer providerArtifactVersion;

    private String providerUri;

    private String providerRequestId;

    private String state;

    private Integer referenceCount;

    private Integer stageAttempts;

    private String lastErrorCode;

    private String lastErrorMessage;

    private String stagingOwner;

    private Date stagingLeaseUntil;

    private Integer version;

    private Date createTime;

    private Date modifyTime;
}
