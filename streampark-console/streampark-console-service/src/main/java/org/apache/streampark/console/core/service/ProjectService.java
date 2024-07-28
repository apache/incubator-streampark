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
import org.apache.streampark.console.base.domain.Result;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ProjectService extends IService<Project> {

    /**
     * Create a new instance.
     *
     * @param project Project to be created
     */
    boolean create(Project project);

    /**
     * Update the given Project
     *
     * @param projectParam The project to be updated
     * @return whether the update is successful
     */
    boolean update(Project projectParam);

    /**
     * remove the given project by project id
     *
     * @param id project id
     * @return whether the remove is successful
     */
    boolean removeById(Long id);

    /**
     * Retrieves a page of {@link Project} objects based on the provided parameters.
     *
     * @param project @param applicationLog The {@link Project} object containing the search criteria.
     * @param restRequest @param request The {@link RestRequest} object used for pagination and
     *     sorting.
     * @return An {@link IPage} containing the retrieved {@link Project} objects.
     */
    IPage<Project> getPage(Project project, RestRequest restRequest);

    /**
     * Check whether the corresponding project exists through team id
     *
     * @param teamId Project contains team id
     * @return whether the corresponding project exists
     */
    Boolean existsByTeamId(Long teamId);

    /**
     * List all project by team id
     *
     * @param teamId Project contains team id
     * @return List of project
     */
    List<Project> listByTeamId(Long teamId);

    /**
     * Build the project
     *
     * @param id Project id
     * @throws Exception
     */
    void build(Long id) throws Exception;

    /**
     * Get the construction log of the specified project
     *
     * @param id Project id
     * @param startOffset startOffset
     */
    Result<?> getBuildLog(Long id, Long startOffset);

    /**
     * List all modules of the specified project
     *
     * @param id Project id
     * @return List of modules
     */
    List<String> listModules(Long id);

    /**
     * List all Jars of the specified project
     *
     * @param project Project
     * @return List of Jars
     */
    List<String> listJars(Project project);

    /**
     * List all project configs of the specified project
     *
     * @param project Project
     * @return List of configs
     */
    List<Map<String, Object>> listConf(Project project);

    /**
     * Get the configuration path of Application
     *
     * @param id Project id
     * @param module Module
     * @return Application config path
     */
    String getAppConfPath(Long id, String module);

    /**
     * List all Application of the specified project
     *
     * @param project Project
     * @return List of Applications
     */
    List<Application> listApps(Project project);

    /**
     * Check whether the corresponding project exists
     *
     * @param project Project
     * @return whether the corresponding project exists
     */
    boolean exists(Project project);

    /**
     * Gets branch information under the project
     *
     * @param project Project
     * @return branch information under the project
     */
    List<String> getAllBranches(Project project);

    /**
     * Check git
     *
     * @param project Project
     * @return Check git
     */
    GitAuthorizedErrorEnum gitCheck(Project project);
}
