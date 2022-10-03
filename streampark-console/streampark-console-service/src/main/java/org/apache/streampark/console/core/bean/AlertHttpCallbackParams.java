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

package org.apache.streampark.console.core.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

@ApiModel(value = "AlertHttpCallback")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertHttpCallbackParams implements Serializable {

    @ApiModelProperty(name = "url", value = "callback url")
    @NotBlank(message = "The url of callback must be not empty")
    private String url;

    @ApiModelProperty(name = "method", value = "http method")
    private String method = "POST";

    @ApiModelProperty(name = "contentType", value = "content type", example = "application/json")
    private String contentType;

    @ApiModelProperty(name = "requestTemplate", value = "use freemarker template replace the params")
    private String requestTemplate;
}
