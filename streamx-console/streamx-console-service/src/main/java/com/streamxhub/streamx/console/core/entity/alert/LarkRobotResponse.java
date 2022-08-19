/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.entity.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The robot alarm response of Lark
 *
 * @author weijinglun
 * @date 2022.06.24
 */
@NoArgsConstructor
@Data
public class LarkRobotResponse {
    @JsonProperty("Extra")
    private Object extra;
    @JsonProperty("StatusCode")
    private Integer statusCode;
    @JsonProperty("StatusMessage")
    private String statusMessage;
    private Integer code;
    private String msg;
    private Object data;
}
