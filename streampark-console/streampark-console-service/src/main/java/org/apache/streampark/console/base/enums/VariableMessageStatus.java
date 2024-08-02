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
public enum VariableMessageStatus implements Status {

    SYSTEM_VARIABLE_ID_NULL_FAILED(10110, "The variable id cannot be null.", "变量Id不能为空"),
    SYSTEM_VARIABLE_NOT_EXIST(10120, "The variable does not exist.", "变量不存在"),
    SYSTEM_VARIABLE_EXIST_USE(10130, "The variable is actually used.", "该变量实际上是在用的"),
    SYSTEM_VARIABLE_ALREADY_EXIST(10140, "The variable code already exists", "变量代码已存在"),
    SYSTEM_VARIABLE_CODE_MODIFY_FAILED(10150, "The variable code cannot be updated.", "变量代码无法更新"),
    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;

}
