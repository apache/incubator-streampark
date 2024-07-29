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
package org.apache.streampark.console.core.service;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.entity.Message;
import org.apache.streampark.console.core.enums.NoticeTypeEnum;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** Message push service */
public interface MessageService extends IService<Message> {

    /**
     * push message to user
     *
     * @param message Message
     */
    void push(Message message);

    /**
     * Retrieves a page of {@link Message} objects based on the provided parameters.
     *
     * @param noticeTypeEnum request request The {@link NoticeTypeEnum} object used for pagination and
     *     sorting.
     * @param request request request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link Message} objects.
     */
    IPage<Message> getUnReadPage(NoticeTypeEnum noticeTypeEnum, RestRequest request);
}
