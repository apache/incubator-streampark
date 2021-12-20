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

package com.streamxhub.streamx.console.core.service;

import java.util.List;

/**
 * @author Whojohn
 * @time 2021.12.20
 */
public interface SqlComplete {
    /**
     * 功能：
     * 1. 传入一个完整 sql 语句(推荐传入截断的字符)，只对最后一词联想
     * 2. 最后一个词的定义是非空格字符
     *
     * @param sql 输入一个需要联想的 sql
     * @return 返回一个潜在词列表
     */
    public List<String> getComplete(String sql);
}
