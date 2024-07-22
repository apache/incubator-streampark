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

package org.apache.streampark.console.system.controller;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.domain.router.VueRouter;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.entity.Menu;
import org.apache.streampark.console.system.service.MenuService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @PostMapping("router")
    public RestResponse getUserRouters(Long teamId) {
        // TODO The teamId is required, get routers should be called after choose teamId.
        List<VueRouter<Menu>> routers = this.menuService.listRouters(ServiceHelper.getUserId(), teamId);
        return RestResponse.success(routers);
    }

    @PostMapping("list")
    @RequiresPermissions("menu:view")
    public RestResponse menuList(Menu menu) {
        Map<String, Object> menuMap = this.menuService.listMenuMap(menu);
        return RestResponse.success(menuMap);
    }
}
