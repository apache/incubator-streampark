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

import org.apache.streampark.console.core.entity.Message;
import org.apache.streampark.console.core.response.message.MessageResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

/** Converts between message entities and API response contracts. */
public final class MessageAssembler {

    private MessageAssembler() {
    }

    public static MessageResponse toResponse(Message message) {
        return DtoAssembler.toDto(message, MessageResponse.class);
    }

    public static IPage<MessageResponse> toPageResponse(IPage<Message> page) {
        return DtoAssembler.toPage(page, MessageAssembler::toResponse);
    }
}
