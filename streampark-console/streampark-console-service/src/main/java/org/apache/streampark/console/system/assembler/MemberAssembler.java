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
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.request.member.MemberCreateRequest;
import org.apache.streampark.console.system.request.member.MemberListQueryRequest;
import org.apache.streampark.console.system.request.member.MemberUpdateRequest;
import org.apache.streampark.console.system.response.member.MemberResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

/** Converts between member entities and API request/response contracts. */
public final class MemberAssembler {

    private MemberAssembler() {
    }

    public static Member toEntity(MemberListQueryRequest request) {
        return DtoAssembler.toDto(request, Member.class);
    }

    public static Member toEntity(MemberCreateRequest request) {
        return DtoAssembler.toDto(request, Member.class);
    }

    public static Member toEntity(MemberUpdateRequest request) {
        return DtoAssembler.toDto(request, Member.class);
    }

    public static MemberResponse toResponse(Member member) {
        return DtoAssembler.toDto(member, MemberResponse.class);
    }

    public static IPage<MemberResponse> toPageResponse(IPage<Member> page) {
        return DtoAssembler.toPage(page, MemberAssembler::toResponse);
    }
}
