/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.base.domain;

/**
 * @author xianwei.yang
 * 业务响应code
 */
public interface ResponseCode {

    /**
     * default
     */
    Long CODE_SUCCESS = 2000L;
    Long CODE_FAIL = 2001L;

    /**
     * 预占：3000-4000为accessToken异常
     */
    Long CODE_ACCESS_TOKEN_LOCKED = 3001L;

}
