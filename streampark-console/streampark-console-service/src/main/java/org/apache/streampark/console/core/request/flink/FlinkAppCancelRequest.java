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

package org.apache.streampark.console.core.request.flink;

import org.apache.streampark.console.core.annotation.ApiParam;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * Request body for {@code POST /flink/app/cancel}, aligned with webapp {@code CancelParam}.
 */
@Getter
@Setter
public class FlinkAppCancelRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @ApiParam(description = "Application id", required = true)
    private Long id;

    @NotNull
    @ApiParam(description = "Team id", required = true)
    private Long teamId;

    @ApiParam(name = "triggerSavepoint", description = "Trigger savepoint before stopping", defaultValue = "false")
    private Boolean restoreOrTriggerSavepoint;

    @ApiParam(description = "Drain pipeline before canceling", defaultValue = "false")
    private Boolean drain;

    private Boolean nativeFormat;

    @ApiParam(description = "Savepoint path")
    private String savepointPath;
}
