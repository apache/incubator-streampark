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

package org.apache.streampark.console.system.request.user;

import org.apache.streampark.console.core.enums.UserTypeEnum;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.io.Serializable;

/** Request body for {@code POST /user/post}. */
@Getter
@Setter
public class UserCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(min = 4, max = 20, message = "{range}")
    private String username;

    private String password;

    @Size(max = 50, message = "{noMoreThan}")
    @Email(message = "{email}")
    private String email;

    private UserTypeEnum userType;

    @NotBlank(message = "{required}")
    private String status;

    @NotBlank(message = "{required}")
    private String sex;

    @Size(max = 100, message = "{noMoreThan}")
    private String description;

    private String nickName;
}
