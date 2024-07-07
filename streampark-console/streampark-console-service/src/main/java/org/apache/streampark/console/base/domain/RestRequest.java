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

package org.apache.streampark.console.base.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springdoc.api.annotations.ParameterObject;

import java.io.Serializable;

@ParameterObject
@Data
public class RestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "10", required = true)
    private int pageSize = 10;

    @Schema(example = "1", required = true)
    private int pageNum = 1;

    @Schema(example = "", description = "e.g. create_time")
    private String sortField = Constant.DEFAULT_SORT_FIELD;

    @Schema(example = "", allowableValues = {"asc", "desc"})
    private String sortOrder = Constant.ORDER_DESC;
}
