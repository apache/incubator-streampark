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

import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.console.base.mybatis.entity.BaseEntity;
import org.apache.streampark.console.core.enums.LoginTypeEnum;
import org.apache.streampark.console.core.enums.UserTypeEnum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /** user status */
    public static final String STATUS_VALID = "1";

    public static final String STATUS_LOCK = "0";

    public static final String SEX_MALE = "0";

    public static final String SEX_FEMALE = "1";

    public static final String SEX_UNKNOWN = "2";

    public static final Integer DEFAULT_PASSWORD_LENGTH = 8;

    public static final String DEFAULT_SECRET = "streampark";

    @TableId(type = IdType.AUTO)
    private Long userId;

    @Size(min = 4, max = 20, message = "{range}")
    private String username;

    private String password;

    @Size(max = 50, message = "{noMoreThan}")
    @Email(message = "{email}")
    private String email;

    private UserTypeEnum userType;

    private LoginTypeEnum loginType;

    @NotBlank(message = "{required}")
    private String status;

    private Date lastLoginTime;

    @NotBlank(message = "{required}")
    private String sex;

    @Size(max = 100, message = "{noMoreThan}")
    private String description;

    private transient String oldPassword;

    private transient String createTimeFrom;
    private transient String createTimeTo;

    private transient String id;

    private String salt;

    private String nickName;

    /** The last set teamId */
    private Long lastTeamId;

    public void dataMasking() {
        String dataMask = Constants.DEFAULT_DATAMASK_STRING;
        this.setPassword(dataMask);
        this.setSalt(dataMask);
    }
}
