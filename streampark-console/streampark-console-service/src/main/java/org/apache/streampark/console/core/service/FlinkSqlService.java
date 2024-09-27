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
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.flink.core.FlinkSqlValidationResult;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** This service is used to handle SQL submission tasks */
public interface FlinkSqlService extends IService<FlinkSql> {

    /**
     * Create FLinkSQL
     *
     * @param flinkSql FlinkSql will create
     */
    void create(FlinkSql flinkSql);

    /**
     * Set Candidate about application and SQL.
     *
     * @param candidateTypeEnum CandidateTypeEnum
     * @param appId application id
     * @param sqlId FlinkSQL id
     */
    void setCandidate(CandidateTypeEnum candidateTypeEnum, Long appId, Long sqlId);

    /**
     * @param appId Application id
     * @param decode Whether to choose decode
     * @return FlinkSql
     */
    FlinkSql getEffective(Long appId, boolean decode);

    /**
     * get latest one FLinkSQL by application id
     *
     * @param appId Application id
     * @param decode Whether to choose decode
     * @return FlinkSql of the latest
     */
    FlinkSql getLatestFlinkSql(Long appId, boolean decode);

    /**
     * Get all historical SQL through Application
     *
     * @param appId Application id
     * @return list of History FLinkSQL
     */
    List<FlinkSql> listFlinkSqlHistory(Long appId);

    /**
     * Get FlinkSQL by Application id and Candidate Type
     *
     * @param appId Application id
     * @param type CandidateTypeEnum
     * @return FlinkSQL
     */
    FlinkSql getCandidate(Long appId, CandidateTypeEnum type);

    /**
     * @param appId Application id
     * @param sqlId FLinkSQL id
     */
    void toEffective(Long appId, Long sqlId);

    /**
     * clean all candidate
     *
     * @param id FlinkSQL id
     */
    void cleanCandidate(Long id);

    /**
     * Remove FLinkSQL by Application id
     *
     * @param appId Application id
     */
    void removeByAppId(Long appId);

    /**
     * FlinkSQL rollback
     *
     * @param application Application
     */
    void rollback(FlinkApplication application);

    /**
     * Verify whether the entered SQL is correct
     *
     * @param sql SQL
     * @param versionId FlinkENV version id
     * @return FlinkSqlValidationResult Check the correctness of SQL
     */
    FlinkSqlValidationResult verifySql(String sql, Long versionId);

    /**
     * List all FlinkSQL by each FLinkSQL team id
     *
     * @param teamId FlinkSQL team id
     * @return list of FlinkSQL
     */
    List<FlinkSql> listByTeamId(Long teamId);

    /**
     * Retrieves a page of {@link FlinkSql} objects based on the provided parameters.
     *
     * @param appId Application id
     * @param request request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link FlinkSql} objects.
     */
    IPage<FlinkSql> getPage(Long appId, RestRequest request);
}
