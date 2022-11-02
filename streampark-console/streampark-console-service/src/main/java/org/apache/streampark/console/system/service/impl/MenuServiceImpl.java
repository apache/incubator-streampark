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

import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.router.RouterMeta;
import org.apache.streampark.console.base.domain.router.RouterTree;
import org.apache.streampark.console.base.domain.router.VueRouter;
import org.apache.streampark.console.base.util.TreeUtils;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.system.entity.Menu;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.mapper.MenuMapper;
import org.apache.streampark.console.system.service.MenuService;
import org.apache.streampark.console.system.service.RoleMenuServie;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleMenuServie roleMenuServie;

    @Override
    public List<String> findUserPermissions(Long userId, Long teamId) {
        User user = Optional.ofNullable(userService.getById(userId))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The userId [%s] not found", userId)));
        // Admin has the permission for all menus.
        if (UserType.ADMIN.equals(user.getUserType())) {
            return this.list().stream().map(Menu::getPerms).collect(Collectors.toList());
        }
        return this.baseMapper.findUserPermissions(userId, teamId);
    }

    @Override
    public List<Menu> findUserMenus(Long userId, Long teamId) {
        User user = Optional.ofNullable(userService.getById(userId))
            .orElseThrow(() -> new IllegalArgumentException(String.format("The userId:[%s] not found", userId)));
        // Admin has the permission for all menus.
        if (UserType.ADMIN.equals(user.getUserType())) {
            return this.list(new LambdaQueryWrapper<Menu>().eq(Menu::getType, "0").orderByAsc(Menu::getOrderNum));
        }
        return this.baseMapper.findUserMenus(userId, teamId);
    }

    @Override
    public Map<String, Object> findMenus(Menu menu) {
        Map<String, Object> result = new HashMap<>(16);
        try {
            LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
            findMenuCondition(queryWrapper, menu);
            List<Menu> menus = baseMapper.selectList(queryWrapper);

            List<RouterTree<Menu>> trees = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            buildTrees(trees, menus, ids);

            result.put("ids", ids);
            if (StringUtils.equals(menu.getType(), Constant.TYPE_BUTTON)) {
                result.put("rows", trees);
            } else {
                RouterTree<Menu> menuTree = TreeUtils.build(trees);
                result.put("rows", menuTree);
            }
            result.put("total", menus.size());
        } catch (NumberFormatException e) {
            log.info("Failed to query menu", e);
            result.put("rows", null);
            result.put("total", 0);
        }
        return result;
    }

    @Override
    public List<Menu> findMenuList(Menu menu) {
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        findMenuCondition(queryWrapper, menu);
        queryWrapper.orderByAsc(Menu::getMenuId);
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(Menu menu) {
        menu.setCreateTime(new Date());
        setMenu(menu);
        this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Menu menu) throws Exception {
        menu.setModifyTime(new Date());
        setMenu(menu);
        baseMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenus(String[] menuIds) throws Exception {
        // Find users associated with these menus/buttons
        this.roleMenuServie.deleteByMenuId(menuIds);
        // Recursively delete these menus/buttons
        this.removeByIds(Arrays.asList(menuIds));
    }

    @Override
    public ArrayList<VueRouter<Menu>> getUserRouters(Long userId, Long teamId) {
        List<VueRouter<Menu>> routes = new ArrayList<>();
        // The query type is the menu type
        List<Menu> menus = this.findUserMenus(userId, teamId);
        menus.forEach(menu -> {
            VueRouter<Menu> route = new VueRouter<>();
            route.setId(menu.getMenuId().toString());
            route.setParentId(menu.getParentId().toString());
            route.setPath(menu.getPath());
            route.setComponent(menu.getComponent());
            route.setName(menu.getMenuName());
            route.setMeta(new RouterMeta(true, !menu.isDisplay(), true, menu.getIcon()));
            routes.add(route);
        });
        return TreeUtils.buildVueRouter(routes);
    }

    private void buildTrees(List<RouterTree<Menu>> trees, List<Menu> menus, List<String> ids) {
        menus.forEach(menu -> {
            ids.add(menu.getMenuId().toString());
            RouterTree<Menu> tree = new RouterTree<>();
            tree.setId(menu.getMenuId().toString());
            tree.setKey(tree.getId());
            tree.setParentId(menu.getParentId().toString());
            tree.setText(menu.getMenuName());
            tree.setTitle(tree.getText());
            tree.setIcon(menu.getIcon());
            tree.setComponent(menu.getComponent());
            tree.setCreateTime(menu.getCreateTime());
            tree.setModifyTime(menu.getModifyTime());
            tree.setPath(menu.getPath());
            tree.setOrder(menu.getOrderNum());
            tree.setPermission(menu.getPerms());
            tree.setType(menu.getType());
            tree.setDisplay(menu.isDisplay());
            trees.add(tree);
        });
    }

    private void setMenu(Menu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (Menu.TYPE_BUTTON.equals(menu.getType())) {
            menu.setPath(null);
            menu.setIcon(null);
            menu.setComponent(null);
        }
    }

    private void findMenuCondition(LambdaQueryWrapper<Menu> queryWrapper, Menu menu) {
        if (StringUtils.isNotBlank(menu.getMenuName())) {
            queryWrapper.eq(Menu::getMenuName, menu.getMenuName());
        }
        if (StringUtils.isNotBlank(menu.getType())) {
            queryWrapper.eq(Menu::getType, menu.getType());
        }
        if (StringUtils.isNotBlank(menu.getCreateTimeFrom())
            && StringUtils.isNotBlank(menu.getCreateTimeTo())) {
            queryWrapper
                .ge(Menu::getCreateTime, menu.getCreateTimeFrom())
                .le(Menu::getCreateTime, menu.getCreateTimeTo());
        }
    }
}
