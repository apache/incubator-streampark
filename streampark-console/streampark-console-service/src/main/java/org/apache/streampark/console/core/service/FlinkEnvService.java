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
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.FlinkEnvCheckEnum;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;

public interface FlinkEnvService extends IService<FlinkEnv> {

    /**
     * Checks if a specific version of Flink exists.
     *
     * @param version The version of Flink to check.
     * @return Returns enum value indicating the existence of the specified version.
     */
    FlinkEnvCheckEnum check(FlinkEnv version);

    /**
     * Create a new instance.
     *
     * @param version The version of FlinkEnv to use.
     * @return true if the instance is successfully created, false otherwise.
     */
    boolean create(FlinkEnv version);

    /**
     * Deletes a Flink environment with the provided ID.
     *
     * @param id the ID of the Flink environment to delete
     */
    void removeById(Long id);

    /**
     * Updates the specified version of Flink environment.
     *
     * @param version the version of Flink environment to update
     */
    void update(FlinkEnv version);

    /**
     * Get flink version by application id.
     *
     * @param appId the ID of the application
     * @return the FlinkEnv object representing the version of Flink associated with the given app ID
     */
    FlinkEnv getByAppId(Long appId);

    /**
     * Sets the specified Flink version as the default.
     *
     * @param id The ID of the Flink version to set as the default.
     */
    void setDefault(Long id);

    /**
     * Retrieves the default version of FlinkEnv.
     *
     * @return the default version of FlinkEnv
     */
    FlinkEnv getDefault();

    /**
     * Retrieves a Flink environment by ID, if available. If the ID is null or not found, the method
     * returns the default Flink environment.
     *
     * @param id The ID of the Flink environment to retrieve. If null, the default environment will be
     *     retrieved.
     * @return The Flink environment with the specified ID, or the default environment if the ID is
     *     null or not found.
     */
    FlinkEnv getByIdOrDefault(Long id);

    /**
     * Synchronizes the configuration file for the given id.
     *
     * @param id The id of the configuration file to be synchronized.
     * @throws IOException If an I/O error occurs while synchronizing the configuration file.
     */
    void syncConf(Long id) throws IOException;

    /**
     * Checks the validity of the given ID.
     *
     * @param id The ID to check for validity.
     */
    void validity(Long id);

    IPage<FlinkEnv> findPage(FlinkEnv flinkEnv, RestRequest restRequest);
}
