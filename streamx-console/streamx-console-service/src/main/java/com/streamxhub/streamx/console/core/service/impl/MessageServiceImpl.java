/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.util.SortUtils;
import com.streamxhub.streamx.console.core.dao.MessageMapper;
import com.streamxhub.streamx.console.core.entity.Message;
import com.streamxhub.streamx.console.core.enums.NoticeType;
import com.streamxhub.streamx.console.core.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
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

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    @Override
    public void push(Message message) {
        save(message);
        simpMessageSendingOperations.convertAndSend("/resp/notice", message);
    }

    @Override
    public IPage<Message> getUnRead(NoticeType noticeType, RestRequest request) {
        Page<Message> page = new Page<>();
        LambdaQueryWrapper<Message> query = new QueryWrapper<Message>().lambda();
        query.eq(Message::getReaded, false).orderByDesc(Message::getCreateTime);
        query.eq(Message::getType, noticeType.get());
        SortUtils.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        return this.baseMapper.selectPage(page, query);
    }
}
