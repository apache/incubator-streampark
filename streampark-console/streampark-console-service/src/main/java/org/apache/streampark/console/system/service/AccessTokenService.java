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

package org.apache.streampark.console.system.service;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.system.entity.AccessToken;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** This interface is related to Token information acquisition and operation */
public interface AccessTokenService extends IService<AccessToken> {

    /**
     * Generate token based on user ID's expiration time and description
     *
     * @param userId User id
     * @param description more description
     * @return RestResponse
     * @throws InternalException
     */
    RestResponse create(Long userId, String description) throws Exception;

    /**
     * Retrieves a page of {@link AccessToken} objects based on the provided parameters.
     *
     * @param tokenParam The {@link AccessToken} object containing the search criteria.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link AccessToken} objects.
     */
    IPage<AccessToken> getPage(AccessToken tokenParam, RestRequest request);

    /**
     * Update information in token
     *
     * @param tokenId AccessToken id
     * @return RestResponse
     */
    RestResponse toggle(Long tokenId);

    /**
     * Get the corresponding AccessToken based on the user ID
     *
     * @param userId User id
     * @return AccessToken
     */
    AccessToken getByUserId(Long userId);
}
