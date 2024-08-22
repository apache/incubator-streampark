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
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkSql;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.spark.core.util.SparkSqlValidationResult;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** This service is used to handle SQL submission tasks */
public interface SparkSqlService extends IService<SparkSql> {

    /**
     * Create SparkSQL
     *
     * @param sparkSql SparkSql will create
     */
    void create(SparkSql sparkSql);

    /**
     * Set Candidate about application and SQL.
     *
     * @param candidateTypeEnum CandidateTypeEnum
     * @param appId application id
     * @param sqlId SparkSQL id
     */
    void setCandidate(CandidateTypeEnum candidateTypeEnum, Long appId, Long sqlId);

    /**
     * @param appId Application id
     * @param decode Whether to choose decode
     * @return SparkSQL
     */
    SparkSql getEffective(Long appId, boolean decode);

    /**
     * get latest one SparkSQL by application id
     *
     * @param appId Application id
     * @param decode Whether to choose decode
     * @return SparkSQL of the latest
     */
    SparkSql getLatestSparkSql(Long appId, boolean decode);

    /**
     * Get all historical SQL through Application
     *
     * @param appId Application id
     * @return list of History SparkSQL
     */
    List<SparkSql> listSparkSqlHistory(Long appId);

    /**
     * Get SparkSQL by Application id and Candidate Type
     *
     * @param appId Application id
     * @param type CandidateTypeEnum
     * @return SparkSQL
     */
    SparkSql getCandidate(Long appId, CandidateTypeEnum type);

    /**
     * @param appId Application id
     * @param sqlId SparkSQL id
     */
    void toEffective(Long appId, Long sqlId);

    /**
     * clean all candidate
     *
     * @param id SparkSQL id
     */
    void cleanCandidate(Long id);

    /**
     * Remove SparkSQL by Application id
     *
     * @param appId Application id
     */
    void removeByAppId(Long appId);

    /**
     * SparkSQL rollback
     *
     * @param application SparkApplication
     */
    void rollback(SparkApplication application);

    /**
     * Verify whether the entered SQL is correct
     *
     * @param sql SQL
     * @param versionId SparkENV version id
     * @return SparkSqlValidationResult Check the correctness of SQL
     */
    SparkSqlValidationResult verifySql(String sql, Long versionId);

    /**
     * List all SparkSQL by each SparkSQL team id
     *
     * @param teamId SparkSQL team id
     * @return list of SparkSQL
     */
    List<SparkSql> listByTeamId(Long teamId);

    /**
     * Retrieves a page of {@link SparkSql} objects based on the provided parameters.
     *
     * @param appId Application id
     * @param request request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link SparkSql} objects.
     */
    IPage<SparkSql> getPage(Long appId, RestRequest request);
}
