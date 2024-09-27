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
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkSavepoint;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.annotation.Nullable;

public interface SavepointService extends IService<FlinkSavepoint> {

    /**
     * Expires all savepoints for the specified application.
     *
     * @param appId the ID of the application to expire
     */
    void expire(Long appId);

    /**
     * Retrieves the latest savepoint based on the given id.
     *
     * @param id the unique identifier of the SavePoint
     * @return the latest SavePoint object, or null if not found
     */
    FlinkSavepoint getLatest(Long id);

    /**
     * Triggers a savepoint for the specified application.
     *
     * @param appId the ID of the application to trigger the savepoint for
     * @param savepointPath the path where the savepoint will be stored, or null if the default path
     *     should be used
     * @param nativeFormat true to store the savepoint in native format, false otherwise
     */
    void trigger(Long appId, @Nullable String savepointPath, @Nullable Boolean nativeFormat);

    /**
     * Deletes an application with the specified ID.
     *
     * @param id the ID of the application to be deleted
     * @param appParam the application object representing the application to be deleted
     * @return true if the application is successfully deleted, false otherwise
     * @throws InternalException if there is an internal error during the deletion process
     */
    Boolean remove(Long id, FlinkApplication appParam) throws InternalException;

    /**
     * Retrieves a page of savepoint objects based on the specified parameters.
     *
     * @param savepoint The SavePoint object to be used for filtering the page results.
     * @param request The RestRequest object containing additional request parameters.
     * @return An instance of IPage<SavePoint> representing the page of SavePoint objects.
     */
    IPage<FlinkSavepoint> getPage(FlinkSavepoint savepoint, RestRequest request);

    /**
     * Removes all savepoints for the specified application.
     *
     * @param appParam the application to be removed
     */
    void remove(FlinkApplication appParam);

    /**
     * Returns the savepoint path for the given application.
     *
     * @param appParam the application for which to get the save point path
     * @return the save point path for the given application
     * @throws Exception if an error occurs while getting the save point path
     */
    String getSavePointPath(FlinkApplication appParam) throws Exception;
}
