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

package com.streamxhub.streamx.console.core.bean;

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
public class AlertHttpCallbackParams implements Serializable {
    @NotBlank(message = "The url of alert must not be empty")
    private String url;
    /**
     * http request method, default is  POST
     */
    private String method = "POST";

    /**
     * http request contentType, default is application/json
     */
    private String contentType;

    /**
     * use freemarker replace the params
     */
    private String requestTemplate;
}
