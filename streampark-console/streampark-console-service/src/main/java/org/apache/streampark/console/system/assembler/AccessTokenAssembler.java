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

package org.apache.streampark.console.system.assembler;

import org.apache.streampark.console.core.assembler.DtoAssembler;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.request.token.TokenListQueryRequest;
import org.apache.streampark.console.system.response.token.AccessTokenResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

/** Converts between access-token entities and API request/response contracts. */
public final class AccessTokenAssembler {

    private AccessTokenAssembler() {
    }

    public static AccessToken toEntity(TokenListQueryRequest request) {
        return DtoAssembler.toDto(request, AccessToken.class);
    }

    public static AccessTokenResponse toResponse(AccessToken token) {
        return DtoAssembler.toDto(token, AccessTokenResponse.class);
    }

    public static IPage<AccessTokenResponse> toPageResponse(IPage<AccessToken> page) {
        return DtoAssembler.toPage(page, AccessTokenAssembler::toResponse);
    }
}
