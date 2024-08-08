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
public enum YarnMessageStatus implements Status {

    YARN_QUEUE_NOT_EXIST(10740, "The yarn queue doesn't exist.", "Yarn队列不存在"),
    YARN_QUEUE_NULL(10750, "Yarn queue mustn't be empty.", "Yarn队列不能为空"),
    YARN_QUEUE_ID_NULL(10760, "Yarn queue id mustn't be empty.", "Yarn队列ID不能为空"),
    YARN_QUEUE_LABEL_EXIST(10770, "The queue label existed already. Try on a new queue label, please.",
        "队列标签已存在，请尝试使用新的队列标签。"),
    YARN_QUEUE_LABEL_NULL(10780, "Yarn queue label mustn't be empty.", "Yarn队列标签不能为空"),
    YARN_QUEUE_LABEL_AVAILABLE(10790, "The queue label is availableThe queue label is available.", "队列标签可用队列标签可用"),
    YARN_QUEUE_LABEL_FORMAT(10800,
        "Yarn queue label format should be in format {queue} or {queue}@{label1，label2}",
        "Yarn队列标签格式应为格式 {queue} 或 {queue}@{label1，label2}"),
    YARN_QUEUE_QUERY_PARAMS_NULL(10810, "Yarn queue query params mustn't be null.", "Yarn队列查询参数不能为空"),
    YARN_QUEUE_QUERY_PARAMS_TEAM_ID_NULL(10820, "Team id of yarn queue query params mustn't be null.",
        "Yarn队列查询参数的团队ID不能为空"),
    YARN_QUEUE_USED_FORMAT(10830, "Please remove the yarn queue for {0} referenced it before {1}.",
        "请在{1}之前删除{0}引用的Yarn队列"),

    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;

}
