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

package org.apache.streampark.console.core.service.application;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkApplicationConfig;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/** This interface defines the methods to manage the application configuration. */
public interface FlinkApplicationConfigService extends IService<FlinkApplicationConfig> {

    /**
     * Creates a new instance of an Application.
     *
     * @param appParam The Application object to create.
     * @param latest If set to true, sets the created Application as the latest version.
     */
    void create(FlinkApplication appParam, Boolean latest);

    /**
     * Updates the given application.
     *
     * @param appParam the application to be updated
     * @param latest a boolean indicating whether to update to the latest version
     */
    void update(FlinkApplication appParam, Boolean latest);

    /**
     * Sets the latest or effective flag for a given configuration and application. The latest flag
     * determines whether the configuration is the latest version available. The effective flag
     * determines whether the configuration is effective for the application.
     *
     * @param latest a boolean value indicating whether the configuration is the latest version (true)
     *     or not (false)
     * @param configId the ID of the configuration
     * @param appId the ID of the application
     */
    void setLatestOrEffective(Boolean latest, Long configId, Long appId);

    /**
     * Sets the configuration to effective for the given application and configuration ID.
     *
     * @param appId The ID of the application
     * @param configId The ID of the configuration
     */
    void toEffective(Long appId, Long configId);

    /**
     * Returns the latest version of the application configuration for the given application ID.
     *
     * @param appId The ID of the application
     * @return The latest version of the application configuration
     */
    FlinkApplicationConfig getLatest(Long appId);

    /**
     * Retrieves the effective ApplicationConfig for the given appId.
     *
     * @param appId The identifier of the application.
     * @return The effective ApplicationConfig.
     */
    FlinkApplicationConfig getEffective(Long appId);

    /**
     * Retrieves the ApplicationConfig for the specified ID.
     *
     * @param id the ID of the ApplicationConfig to retrieve
     * @return the ApplicationConfig object corresponding to the specified ID, or null if no
     *     ApplicationConfig is found
     */
    FlinkApplicationConfig get(Long id);

    /**
     * Retrieves a page of ApplicationConfig objects based on the specified ApplicationConfig and
     * RestRequest.
     *
     * @param config the ApplicationConfig object to use as a filter for retrieving the page
     * @param request the RestRequest object containing additional parameters and settings for
     *     retrieving the page
     * @return an IPage containing the ApplicationConfig objects that match the filter criteria
     *     specified in the config object, limited by the settings in the request object
     */
    IPage<FlinkApplicationConfig> getPage(FlinkApplicationConfig config, RestRequest request);

    /**
     * Retrieves the history of application configurations for a given application.
     *
     * @param appId The application's id for which to retrieve the history.
     * @return The list of application configurations representing the history.
     */
    List<FlinkApplicationConfig> list(Long appId);

    /**
     * Reads a template from a file or a database.
     *
     * @return the content of the template as a String
     */
    String readTemplate();

    /**
     * Removes the app with the specified appId.
     *
     * @param appId The id of the app to be removed.
     */
    void removeByAppId(Long appId);
}
