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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Message;
import org.apache.streampark.console.core.enums.NoticeType;
import org.apache.streampark.console.core.service.MessageService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MESSAGE_TAG")
@Slf4j
@Validated
@RestController
@RequestMapping("message")
public class MessageController {

  @Autowired private MessageService messageService;

  @Operation(summary = "List notices")
  @PostMapping("notice")
  public RestResponse notice(Integer type, RestRequest request) {
    NoticeType noticeType = NoticeType.of(type);
    IPage<Message> pages = messageService.getUnRead(noticeType, request);
    return RestResponse.success(pages);
  }

  @Operation(summary = "Delete notice")
  @PostMapping("delnotice")
  public RestResponse delNotice(Long id) {
    return RestResponse.success(messageService.removeById(id));
  }
}
