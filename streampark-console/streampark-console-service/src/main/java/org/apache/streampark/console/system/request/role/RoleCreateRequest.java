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

package org.apache.streampark.console.system.request.role;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.io.Serializable;

/** Request body for {@code POST /role/post}. */
@Getter
@Setter
public class RoleCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{required}")
    @Size(max = 10, message = "{noMoreThan}")
    private String roleName;

    @Size(max = 255, message = "{noMoreThan}")
    private String description;

    /** Comma-separated menu ids, aligned with webapp {@code RoleParam.menuId}. */
    private String menuId;
}
