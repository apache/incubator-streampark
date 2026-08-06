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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.web.FormOrJson;
import org.apache.streampark.console.core.assembler.MessageAssembler;
import org.apache.streampark.console.core.entity.Message;
import org.apache.streampark.console.core.enums.NoticeTypeEnum;
import org.apache.streampark.console.core.request.message.MessageDeleteRequest;
import org.apache.streampark.console.core.request.message.MessageNoticeRequest;
import org.apache.streampark.console.core.response.message.MessageResponse;
import org.apache.streampark.console.core.service.MessageService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("notice")
    public RestResponseBody<IPage<MessageResponse>> notice(@Valid MessageNoticeRequest request,
                                                           RestRequest restRequest) {
        NoticeTypeEnum noticeTypeEnum = NoticeTypeEnum.of(request.getType());
        IPage<Message> pages = messageService.getUnReadPage(noticeTypeEnum, restRequest);
        return RestResponseBody.success(MessageAssembler.toPageResponse(pages));
    }

    @PostMapping("delete")
    public RestResponseBody<Boolean> delete(@Valid @FormOrJson MessageDeleteRequest request) {
        return RestResponseBody.success(messageService.removeById(request.getId()));
    }
}
