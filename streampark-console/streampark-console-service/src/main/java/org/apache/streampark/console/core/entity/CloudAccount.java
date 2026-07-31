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

import org.apache.streampark.console.base.mybatis.entity.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** Encrypted credential metadata for a managed Flink cloud account. */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("t_cloud_account")
public class CloudAccount extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountName;

    private String providerType;

    private String region;

    private String endpoint;

    @JsonIgnore
    private String accessKeyCiphertext;

    @JsonIgnore
    private String secretKeyCiphertext;

    private Integer credentialKeyVersion;

    private String accessKeyMask;

    private Integer connectivityState;

    private Date lastCheckTime;

    private String lastErrorCode;

    private String lastErrorMessage;

    private Integer status;

    private String description;

    private Long createUserId;

    private Integer version;
}
