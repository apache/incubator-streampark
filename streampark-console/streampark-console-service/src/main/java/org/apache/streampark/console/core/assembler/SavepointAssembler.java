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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.entity.FlinkSavepoint;
import org.apache.streampark.console.core.request.flink.SavepointDeleteRequest;
import org.apache.streampark.console.core.request.flink.SavepointHistoryQueryRequest;
import org.apache.streampark.console.core.response.flink.SavepointResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

/**
 * Converts between Flink savepoint entities and API request/response contracts.
 */
public final class SavepointAssembler {

    private SavepointAssembler() {
    }

    public static FlinkSavepoint toEntity(SavepointHistoryQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkSavepoint savepoint = new FlinkSavepoint();
        savepoint.setAppId(request.getAppId());
        savepoint.setTeamId(request.getTeamId());
        return savepoint;
    }

    public static FlinkSavepoint toEntity(SavepointDeleteRequest request) {
        if (request == null) {
            return null;
        }
        FlinkSavepoint savepoint = new FlinkSavepoint();
        savepoint.setAppId(request.getAppId());
        savepoint.setTeamId(request.getTeamId());
        savepoint.setId(request.getId());
        return savepoint;
    }

    public static SavepointResponse toResponse(FlinkSavepoint savepoint) {
        if (savepoint == null) {
            return null;
        }
        SavepointResponse response = new SavepointResponse();
        BeanUtils.copyProperties(savepoint, response);
        return response;
    }

    public static IPage<SavepointResponse> toPageResponse(IPage<FlinkSavepoint> page) {
        return DtoAssembler.toPage(page, SavepointAssembler::toResponse);
    }
}
