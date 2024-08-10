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

package org.apache.streampark.console.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProjectMessageStatus implements Status {

    PROJECT_MODULE_NULL_ERROR(101070, "Project module can't be null, please check.", "项目模块不能为空，请检查"),
    PROJECT_NAME_EXIST(101080, "project name already exists", "项目名称已存在"),
    PROJECT_GIT_PASSWORD_DECRYPT_FAILED(101090, "Project Github/Gitlab password decrypt failed",
        "项目 Github/Gitlab 密码解密失败"),
    PROJECT_TEAM_ID_MODIFY_ERROR(101100, "TeamId can't be changed", "无法更改TeamId"),
    PROJECT_BUILDING_STATE(101110, "The project is being built", "该项目正在建设中"),
    PROJECT_RUNNING_BUILDING_EXCEED_LIMIT(101120,
        "The number of running Build projects exceeds the maximum number: {0}",
        "正在运行的Build项目数超过最大数量: {0}"),

    API_NOT_SUPPORT(101150, "current api unsupported: {0}", "当前API不受支持: {0}"),
    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;

}
