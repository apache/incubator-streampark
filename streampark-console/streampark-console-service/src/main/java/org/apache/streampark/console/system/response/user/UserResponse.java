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

package org.apache.streampark.console.system.response.user;

import org.apache.streampark.console.core.enums.LoginTypeEnum;
import org.apache.streampark.console.core.enums.UserTypeEnum;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * API response for a user record, aligned with webapp {@code UserListRecord}.
 */
@Getter
@Setter
public class UserResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String email;

    private UserTypeEnum userType;

    private LoginTypeEnum loginType;

    private String status;

    private Date createTime;

    private Date modifyTime;

    private Date lastLoginTime;

    private String sex;

    private String description;

    private String nickName;

    private Long lastTeamId;

    private String id;
}
