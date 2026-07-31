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

/** Durable state for one managed Flink control-plane write operation. */
@Getter
@Setter
@TableName("t_managed_flink_operation")
public class ManagedFlinkOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    private String operationType;

    private String idempotencyKey;

    private String requestHash;

    private Long parentOperationId;

    private String providerRequestId;

    private String providerOperationId;

    private String state;

    private String requestJson;

    private String resultJson;

    private String errorCode;

    private String errorMessage;

    private Integer retryCount;

    private Date nextRetryTime;

    private String executionOwner;

    private Date executionLeaseUntil;

    private Long createUserId;

    private Date createTime;

    private Date startTime;

    private Date finishTime;

    private Date modifyTime;

    private Integer version;
}
