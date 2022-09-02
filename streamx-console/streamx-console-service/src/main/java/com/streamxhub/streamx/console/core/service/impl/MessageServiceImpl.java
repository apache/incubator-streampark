/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.mybatis.pager.MybatisPager;
import com.streamxhub.streamx.console.core.entity.Message;
import com.streamxhub.streamx.console.core.enums.NoticeType;
import com.streamxhub.streamx.console.core.mapper.MessageMapper;
import com.streamxhub.streamx.console.core.service.MessageService;
import com.streamxhub.streamx.console.core.websocket.WebSocketEndpoint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService {

    @Override
    public void push(Message message) {
        save(message);
        WebSocketEndpoint.pushNotice(message);
    }

    @Override
    public IPage<Message> getUnRead(NoticeType noticeType, RestRequest request) {
        LambdaQueryWrapper<Message> query = new QueryWrapper<Message>().lambda();
        query.eq(Message::getRead, false).orderByDesc(Message::getCreateTime);
        query.eq(Message::getType, noticeType.get());
        return this.baseMapper.selectPage(new MybatisPager<Message>().getDefaultPage(request), query);
    }
}
