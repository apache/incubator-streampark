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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.system.entity.Variable;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface VariableService extends IService<Variable> {

    /**
     * find variable
     *
     * @param variable        variable
     * @param restRequest queryRequest
     * @return IPage
     */
    IPage<Variable> findVariables(Variable variable, RestRequest restRequest);

    /**
     * get variables through team
     * @param teamId
     * @return
     */
    List<Variable> findByTeamId(Long teamId);

    /**
     * create variable
     *
     * @param variable variable
     */
    void createVariable(Variable variable) throws Exception;

    /**
     * delete variable
     *
     * @param variable variable
     */
    void deleteVariable(Variable variable) throws Exception;

    /**
     * update variable
     *
     * @param variable variable
     */
    void updateVariable(Variable variable) throws Exception;

    Variable findByVariableCode(Long teamId, String variableCode);

    boolean isExists(Variable variable);

    List<Application> findDependByCode(Variable variable);
}
