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

package org.apache.streampark.console.system.service.impl;

import org.apache.streampark.console.base.bean.router.RouterMeta;
import org.apache.streampark.console.base.bean.router.RouterTree;
import org.apache.streampark.console.base.bean.router.VueRouter;
import org.apache.streampark.console.base.util.VueRouterUtils;
import org.apache.streampark.console.core.enums.UserTypeEnum;
import org.apache.streampark.console.system.entity.Menu;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.MenuMapper;
import org.apache.streampark.console.system.service.MenuService;
import org.apache.streampark.console.system.service.UserService;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    public static final String IDS = "ids";
    public static final String ROWS = "rows";
    public static final String TOTAL = "total";

    @Autowired
    private UserService userService;

    @Override
    public List<String> listPermissions(Long userId, Long teamId) {
        User user = Optional.ofNullable(userService.getById(userId))
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("The userId [%s] not found", userId)));
        // Admin has the permission for all menus.
        if (UserTypeEnum.ADMIN == user.getUserType()) {
            return this.list().stream().map(Menu::getPerms).collect(Collectors.toList());
        }
        return this.baseMapper.selectPermissions(userId, teamId);
    }

    @Override
    public List<Menu> listMenus(Long userId, Long teamId) {
        User user = Optional.ofNullable(userService.getById(userId))
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("The userId:[%s] not found", userId)));
        // Admin has the permission for all menus.
        if (UserTypeEnum.ADMIN == user.getUserType()) {
            LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<Menu>().eq(Menu::getType, "0")
                .orderByAsc(Menu::getOrderNum);
            return this.list(queryWrapper);
        }
        return this.baseMapper.selectMenus(userId, teamId);
    }

    @Override
    public Map<String, Object> listMenuMap(Menu menu) {
        Map<String, Object> result = new HashMap<>(16);
        try {
            LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.isNotBlank(menu.getMenuName())) {
                queryWrapper.eq(Menu::getMenuName, menu.getMenuName());
            }
            if (StringUtils.isNotBlank(menu.getCreateTimeFrom())
                && StringUtils.isNotBlank(menu.getCreateTimeTo())) {
                queryWrapper
                    .ge(Menu::getCreateTime, menu.getCreateTimeFrom())
                    .le(Menu::getCreateTime, menu.getCreateTimeTo());
            }
            List<Menu> menus = baseMapper.selectList(queryWrapper);

            List<RouterTree<Menu>> trees = new ArrayList<>();
            List<String> ids = new ArrayList<>();

            menus.forEach(
                m -> {
                    ids.add(m.getMenuId().toString());
                    trees.add(new RouterTree(m));
                });
            result.put(IDS, ids);
            result.put(TOTAL, menus.size());
            RouterTree<Menu> routerTree = VueRouterUtils.buildRouterTree(trees);
            result.put(ROWS, routerTree);
        } catch (Exception e) {
            log.error("Failed to query menu", e);
            result.put(ROWS, null);
            result.put(TOTAL, 0);
        }
        return result;
    }

    @Override
    public List<VueRouter<Menu>> listRouters(Long userId, Long teamId) {
        List<VueRouter<Menu>> routes = new ArrayList<>();
        // The query type is the menu type
        List<Menu> menus = this.listMenus(userId, teamId);
        menus.forEach(
            menu -> {
                VueRouter<Menu> route = new VueRouter<>();
                route.setId(menu.getMenuId().toString());
                route.setParentId(menu.getParentId().toString());
                route.setPath(menu.getPath());
                route.setComponent(menu.getComponent());
                route.setName(menu.getMenuName());
                route.setMeta(new RouterMeta(true, !menu.isDisplay(), true, menu.getIcon()));
                routes.add(route);
            });
        return VueRouterUtils.buildVueRouter(routes);
    }
}
