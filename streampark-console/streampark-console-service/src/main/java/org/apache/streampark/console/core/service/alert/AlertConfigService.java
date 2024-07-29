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
package org.apache.streampark.console.core.service.alert;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.entity.AlertConfig;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** This interface defines operations related to alarm configuration. */
public interface AlertConfigService extends IService<AlertConfig> {

    /**
     * Retrieves a page of {@link AlertConfigParams} objects based on the provided parameters.
     *
     * @param userId user id.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link AlertConfigParams} objects.
     */
    IPage<AlertConfigParams> page(Long userId, RestRequest request);

    /**
     * check whether the relevant alarm configuration exists
     *
     * @param alertConfig AlertConfig to be checked
     * @return Whether exist in database
     */
    boolean exist(AlertConfig alertConfig);

    /**
     * Remove based on the id configured in the alert
     *
     * @param id AlertConfig id
     * @return Whether removed is successful
     * @throws AlertException
     */
    boolean removeById(Long id) throws AlertException;
}
