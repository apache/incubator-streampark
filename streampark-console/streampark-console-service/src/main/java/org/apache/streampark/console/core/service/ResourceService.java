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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.bean.UploadResponse;
import org.apache.streampark.console.core.entity.Resource;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ResourceService extends IService<Resource> {

    /**
     * list resource
     *
     * @param resource resource
     * @param restRequest queryRequest
     * @return IPage
     */
    IPage<Resource> getPage(Resource resource, RestRequest restRequest);

    /**
     * check resource exists by user id
     *
     * @param userId user id
     * @return true if exists
     */
    boolean existsByUserId(Long userId);

    /**
     * add resource
     *
     * @param resource resource
     */
    void addResource(Resource resource) throws Exception;

    /**
     * @param teamId team id
     * @param name resource name
     * @return the found resource
     */
    Resource findByResourceName(Long teamId, String name);

    /**
     * update resource
     *
     * @param resource the updated resource
     */
    void updateResource(Resource resource);

    /**
     * delete resource
     *
     * @param id
     */
    void remove(Long id);

    /**
     * Get resource through team id.
     *
     * @param teamId
     * @return team resources
     */
    List<Resource> listByTeamId(Long teamId);

    /**
     * change resource owner
     *
     * @param userId original user id
     * @param targetUserId target user id
     */
    void changeOwnership(Long userId, Long targetUserId);

    UploadResponse upload(MultipartFile file) throws IOException;

    RestResponse checkResource(Resource resource) throws Exception;

    /**
     * Uploads a list of jars to the server for historical reference.
     *
     * @return A list of strings representing the names of the uploaded jars.
     */
    List<String> listHistoryUploadJars();

}
