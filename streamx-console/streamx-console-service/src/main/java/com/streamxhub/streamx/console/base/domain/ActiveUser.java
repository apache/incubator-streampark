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
package com.streamxhub.streamx.console.base.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamxhub.streamx.console.base.utils.DateUtil;

/**
 * 在线用户
 *
 * @author benjobs
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActiveUser implements Serializable {
    private static final long serialVersionUID = 2055229953429884344L;

    /**
     * 唯一编号
     */
    private String id = RandomStringUtils.randomAlphanumeric(20);
    /**
     * 用户名
     */
    private String username;
    /**
     * ip地址
     */
    private String ip;
    /**
     * token(加密后)
     */
    private String token;
    /**
     * 登录时间
     */
    private String loginTime =
            DateUtil.formatFullTime(LocalDateTime.now(), DateUtil.FULL_TIME_SPLIT_PATTERN);
    /**
     * 登录地点
     */
    private String loginAddress;
}
