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

import org.apache.streampark.console.core.bean.ResponseResult;
import org.apache.streampark.console.core.entity.YarnQueue;
import org.apache.streampark.console.core.request.yarn.YarnQueueCreateRequest;
import org.apache.streampark.console.core.request.yarn.YarnQueueDeleteRequest;
import org.apache.streampark.console.core.request.yarn.YarnQueueListQueryRequest;
import org.apache.streampark.console.core.request.yarn.YarnQueueUpdateRequest;
import org.apache.streampark.console.core.response.yarn.YarnQueueCheckResponse;
import org.apache.streampark.console.core.response.yarn.YarnQueueResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

/** Converts between yarn queue entities and API request/response contracts. */
public final class YarnQueueAssembler {

    private YarnQueueAssembler() {
    }

    public static YarnQueue toEntity(YarnQueueCreateRequest request) {
        if (request == null) {
            return null;
        }
        YarnQueue yarnQueue = new YarnQueue();
        BeanUtils.copyProperties(request, yarnQueue);
        return yarnQueue;
    }

    public static YarnQueue toEntity(YarnQueueUpdateRequest request) {
        if (request == null) {
            return null;
        }
        YarnQueue yarnQueue = toEntity((YarnQueueCreateRequest) request);
        yarnQueue.setId(request.getId());
        return yarnQueue;
    }

    public static YarnQueue toEntity(YarnQueueListQueryRequest request) {
        if (request == null) {
            return null;
        }
        YarnQueue yarnQueue = new YarnQueue();
        BeanUtils.copyProperties(request, yarnQueue);
        return yarnQueue;
    }

    public static YarnQueue toEntity(YarnQueueDeleteRequest request) {
        if (request == null) {
            return null;
        }
        YarnQueue yarnQueue = new YarnQueue();
        yarnQueue.setId(request.getId());
        yarnQueue.setTeamId(request.getTeamId());
        return yarnQueue;
    }

    public static YarnQueueResponse toResponse(YarnQueue yarnQueue) {
        return DtoAssembler.toDto(yarnQueue, YarnQueueResponse.class);
    }

    public static IPage<YarnQueueResponse> toPageResponse(IPage<YarnQueue> page) {
        return DtoAssembler.toPage(page, YarnQueueAssembler::toResponse);
    }

    public static YarnQueueCheckResponse toCheckResponse(ResponseResult<String> checkResult) {
        if (checkResult == null) {
            return null;
        }
        YarnQueueCheckResponse response = new YarnQueueCheckResponse();
        response.setStatus(checkResult.getStatus());
        response.setMsg(checkResult.getMsg());
        response.setResult(checkResult.getResult());
        return response;
    }
}
