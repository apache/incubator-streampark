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

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.core.entity.FstTree;
import com.streamxhub.streamx.console.core.service.SqlComplete;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author john
 * @time 2021.12.20
 */
@Slf4j
@Service
public class SqlCompleteImpl implements SqlComplete {
    private static final Set<Character> BLACK_SET = new HashSet<Character>();

    {
        BLACK_SET.add(' ');
        BLACK_SET.add(';');
    }

    @Autowired
    FstTree fstTree;

    @Override
    public List<String> getComplete(String sql) {
        // 空格不需要提示
        if (sql.length() > 0 && BLACK_SET.contains(sql.charAt(sql.length() - 1))) {
            return new ArrayList<>();
        }
        String[] temp = sql.split("\\s");
        return fstTree.getComplicate(temp[temp.length - 1]);
    }
}
