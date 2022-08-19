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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LarkParams implements Serializable {
    @NotBlank(message = "The access token of Lark must not be empty")
    private String token;

    /**
     * 是否@所有人
     */
    private Boolean isAtAll = false;

    /**
     * 飞书机器人是否启用加签，默认 false，启用加签需设置 secret_token
     */
    private Boolean secretEnable = false;

    /**
     * 飞书机器人 WebHook 地址的 secret_token,群机器人加签用
     */
    private String secretToken;
}
