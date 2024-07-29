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

import org.apache.streampark.console.base.bean.router.VueRouter;
import org.apache.streampark.console.system.entity.Menu;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/** This interface is used to control the menu of the page */
public interface MenuService extends IService<Menu> {

    /**
     * Get the permissions of current userId.
     *
     * @param userId the user Id
     * @param teamId team id. If it's null, will find permissions from all teams.
     * @return permissions
     */
    List<String> listPermissions(Long userId, Long teamId);

    /**
     * * List menus based on user id and team id
     *
     * @param userId user id
     * @param teamId team id
     * @return List of Menu
     */
    List<Menu> listMenus(Long userId, Long teamId);

    /**
     * List menus map based on menu
     *
     * @param menu Menu
     * @return Menus Map
     */
    Map<String, Object> listMenuMap(Menu menu);

    /**
     * List Menu Routers based on user id and team id
     *
     * @param userId user id
     * @param teamId team id
     * @return List of Menu VueRouter
     */
    List<VueRouter<Menu>> listRouters(Long userId, Long teamId);
}
