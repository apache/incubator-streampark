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

package org.apache.streampark.console.core.response.project;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** API response for a project, aligned with webapp {@code ProjectRecord}. */
@Getter
@Setter
public class ProjectResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long teamId;

    private String name;

    private String url;

    private String refs;

    private Date lastBuild;

    private String userName;

    private String password;

    private String prvkeyPath;

    private Integer repository;

    private String pom;

    private String buildArgs;

    private String description;

    private Integer buildState;

    private Integer type;

    private Date createTime;

    private Date modifyTime;

    private String module;

    private String dateFrom;

    private String dateTo;
}
