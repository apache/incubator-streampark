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

import org.apache.streampark.console.core.entity.ExternalLink;
import org.apache.streampark.console.core.request.externallink.ExternalLinkCreateRequest;
import org.apache.streampark.console.core.request.externallink.ExternalLinkUpdateRequest;
import org.apache.streampark.console.core.response.externallink.ExternalLinkResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.List;

/** Converts between external link entities and API request/response contracts. */
public final class ExternalLinkAssembler {

    private ExternalLinkAssembler() {
    }

    public static ExternalLink toEntity(ExternalLinkCreateRequest request) {
        if (request == null) {
            return null;
        }
        ExternalLink externalLink = new ExternalLink();
        BeanUtils.copyProperties(request, externalLink);
        return externalLink;
    }

    public static ExternalLink toEntity(ExternalLinkUpdateRequest request) {
        if (request == null) {
            return null;
        }
        ExternalLink externalLink = toEntity((ExternalLinkCreateRequest) request);
        externalLink.setId(request.getId());
        return externalLink;
    }

    public static ExternalLinkResponse toResponse(ExternalLink externalLink) {
        return DtoAssembler.toDto(externalLink, ExternalLinkResponse.class);
    }

    public static List<ExternalLinkResponse> toListResponse(List<ExternalLink> externalLinks) {
        return DtoAssembler.toList(externalLinks, ExternalLinkAssembler::toResponse);
    }

    public static IPage<ExternalLinkResponse> toPageResponse(IPage<ExternalLink> page) {
        return DtoAssembler.toPage(page, ExternalLinkAssembler::toResponse);
    }
}
