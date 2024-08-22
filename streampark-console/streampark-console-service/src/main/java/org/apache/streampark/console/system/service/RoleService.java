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
import org.apache.streampark.console.system.entity.Role;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RoleService extends IService<Role> {

    /**
     * Retrieves a page of {@link Role} objects based on the provided parameters.
     *
     * @param role The {@link Role} object containing the search criteria.
     * @param request The {@link RestRequest} object used for pagination and sorting.
     * @return An {@link IPage} containing the retrieved {@link Role} objects.
     */
    IPage<Role> getPage(Role role, RestRequest request);

    /**
     * Get the Role by role name
     *
     * @param roleName role name
     * @return Role
     */
    Role getByName(String roleName);

    /**
     * Create role instance
     *
     * @param role Role
     */
    void createRole(Role role);

    /**
     * Remove Role by role id
     *
     * @param roleId role id
     */
    void removeById(Long roleId);

    /**
     * Update Role with incoming information
     *
     * @param role Role
     */
    void updateRole(Role role);

    /**
     * Get the Default Role
     *
     */
    Role getSysDefaultRole();
}
