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
import org.apache.streampark.console.core.entity.Variable;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface VariableService extends IService<Variable> {

    /**
     * Find variables based on the given variable and query request.
     *
     * @param variable The variable to search for.
     * @param restRequest The query request containing search filters and pagination options.
     * @return An IPage object containing the found Variable objects matching the search criteria.
     */
    IPage<Variable> getPage(Variable variable, RestRequest restRequest);

    /**
     * Retrieves a list of variables based on the team ID.
     *
     * @param teamId The ID of the team to filter the variables by.
     * @return A list of variables that belong to the specified team.
     */
    List<Variable> listByTeamId(Long teamId);

    /**
     * Retrieve a list of variables based on the team ID and search keywords.
     *
     * @param teamId The ID of the team for which to retrieve the variables.
     * @param keyword The fuzzy search keywords used to filter the variables. This parameter is
     *     nullable.
     * @return A List of Variable objects that match the specified team ID and search keywords.
     */
    List<Variable> listByTeamId(Long teamId, String keyword);

    /**
     * Check if a team exists by teamId.
     *
     * @param teamId the id of the team to check.
     * @return true if a team exists with the given teamId, false otherwise.
     */
    boolean existsByTeamId(Long teamId);

    /**
     * Create a variable.
     *
     * @param variable The variable to be created.
     */
    void createVariable(Variable variable);

    /**
     * Deletes a Variable.
     *
     * @param variable the Variable object to be deleted
     */
    void remove(Variable variable);

    /**
     * Find a Variable by its code and team ID.
     *
     * @param teamId The ID of the team to search within.
     * @param variableCode The code of the variable to find.
     * @return The Variable found, or null if no match is found.
     */
    Variable findByVariableCode(Long teamId, String variableCode);

    /**
     * Replaces a specified variable in the given string with the corresponding variable value.
     *
     * @param teamId The identifier of the team.
     * @param mixed The string that may contain variables to be replaced.
     * @return The modified string after replacing the variables.
     */
    String replaceVariable(Long teamId, String mixed);

    /**
     * Retrieves a page of dependent applications based on the given variable and request.
     *
     * @param variable The variable to use for retrieving dependent applications.
     * @param request The REST request containing additional parameters for retrieving the page.
     * @return An instance of IPage<Application> containing the dependent applications.
     */
    IPage<FlinkApplication> getDependAppsPage(Variable variable, RestRequest request);

    /**
     * Updates the given variable.
     *
     * @param variable the variable to be updated
     */
    void updateVariable(Variable variable);
}
