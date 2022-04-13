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

import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.enums.CandidateType;
import com.streamxhub.streamx.flink.core.SqlError;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author benjobs
 */
public interface FlinkSqlService extends IService<FlinkSql> {

    /**
     * @param flinkSql
     * @param latest   是否latest
     */
    void create(FlinkSql flinkSql);

    /**
     * @param latest true  表示设置新增的的记录为 "latest"<br>
     *               false 表示设置新增的的记录为 "Effective"<br>
     * @param sqlId
     * @param appId
     */
    void setCandidate(CandidateType candidateType, Long appId, Long sqlId);

    /**
     * @param appId
     * @param decode
     * @return
     */
    FlinkSql getEffective(Long appId, boolean decode);

    /**
     * @param application
     * @return
     */
    List<FlinkSql> history(Application application);

    /**
     * @param appId
     * @return
     */
    FlinkSql getCandidate(Long appId, CandidateType type);

    /**
     * @param appId
     */
    void toEffective(Long appId, Long sqlId);

    void cleanCandidate(Long id);

    void removeApp(Long appId);

    void rollback(Application application);

    SqlError verifySql(String sql, Long versionId);
}
