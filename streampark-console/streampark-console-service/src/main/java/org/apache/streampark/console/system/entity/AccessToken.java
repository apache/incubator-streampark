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

package org.apache.streampark.console.system.entity;

import org.apache.streampark.console.base.mybatis.entity.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("t_access_token")
public class AccessToken extends BaseEntity {

    public static final String IS_API_TOKEN = "is_api_token";

    public static final Integer STATUS_ENABLE = 1;
    public static final Integer STATUS_DISABLE = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "{required}")
    private Long userId;

    @NotBlank(message = "{required}")
    private String token;

    @NotNull(message = "{required}")
    private Integer status;

    @NotNull(message = "{required}")
    private Date expireTime;

    private String description;

    private transient String username;

    private transient String userStatus;

    /** token final status, 1: available, 0: unavailable */
    private transient Integer finalStatus;

    public AccessToken setStatus(Integer status) {
        this.status = status;
        return this;
    }
}
