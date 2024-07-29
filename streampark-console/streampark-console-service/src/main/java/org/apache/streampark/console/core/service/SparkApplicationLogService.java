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
import org.apache.streampark.console.core.entity.SparkApplicationLog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** This interface is used to record spark application operation logs */
public interface SparkApplicationLogService extends IService<SparkApplicationLog> {

    /**
     * Retrieves a page of {@link SparkApplicationLog} objects based on the provided parameters.
     *
     * @param sparkApplicationLog The {@link SparkApplicationLog} object containing the search criteria.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link SparkApplicationLog} objects.
     */
    IPage<SparkApplicationLog> getPage(SparkApplicationLog sparkApplicationLog, RestRequest request);

    /**
     * remove application log by application id
     *
     * @param appId The id of the application to be removed
     */
    void removeByAppId(Long appId);
}
