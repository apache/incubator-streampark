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

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.enums.CandidateType;
import org.apache.streampark.flink.core.FlinkSqlValidationResult;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface FlinkSqlService extends IService<FlinkSql> {

    void create(FlinkSql flinkSql);

    void setCandidate(CandidateType candidateType, Long appId, Long sqlId);

    FlinkSql getEffective(Long appId, boolean decode);

    FlinkSql getLatestFlinkSql(Long appId, boolean decode);

    List<FlinkSql> history(Application application);

    FlinkSql getCandidate(Long appId, CandidateType type);

    void toEffective(Long appId, Long sqlId);

    void cleanCandidate(Long id);

    void removeApp(Long appId);

    void rollback(Application application);

    FlinkSqlValidationResult verifySql(String sql, Long versionId);

    List<FlinkSql> getByTeamId(Long teamId);
}
