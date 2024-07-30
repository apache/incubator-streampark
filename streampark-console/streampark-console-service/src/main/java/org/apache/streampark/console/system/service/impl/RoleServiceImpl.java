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
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.system.entity.Role;
import org.apache.streampark.console.system.entity.RoleMenu;
import org.apache.streampark.console.system.mapper.RoleMapper;
import org.apache.streampark.console.system.mapper.RoleMenuMapper;
import org.apache.streampark.console.system.service.MemberService;
import org.apache.streampark.console.system.service.RoleMenuService;
import org.apache.streampark.console.system.service.RoleService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_ROLE_EXIST_USED_DELETE_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.SYSTEM_ROLE_NOT_EXIST;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RoleMenuService roleMenuService;

    @Override
    public IPage<Role> getPage(Role role, RestRequest request) {
        Page<Role> page = MybatisPager.getPage(request);
        return this.baseMapper.selectPage(page, role);
    }

    @Override
    public Role getByName(String roleName) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, roleName));
    }

    @Override
    public void createRole(Role role) {
        this.save(role);

        String[] menuIds = role.getMenuId().split(StringPool.COMMA);
        updateRoleMenus(role, menuIds);
    }

    @Override
    public void removeById(Long roleId) {
        Role role = this.getById(roleId);
        ApiAlertException.throwIfNull(role, SYSTEM_ROLE_NOT_EXIST);
        List<Long> userIdsByRoleId = memberService.listUserIdsByRoleId(roleId);
        ApiAlertException.throwIfFalse(
            CollectionUtils.isEmpty(userIdsByRoleId),
            SYSTEM_ROLE_EXIST_USED_DELETE_ERROR,
            role.getRoleName());
        super.removeById(roleId);
        this.roleMenuService.removeByRoleId(roleId);
    }

    @Override
    public void updateRole(Role role) {
        baseMapper.updateById(role);
        LambdaQueryWrapper<RoleMenu> queryWrapper = new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId,
            role.getRoleId());
        roleMenuMapper.delete(queryWrapper);

        String menuId = role.getMenuId();
        if (StringUtils.contains(menuId, Constant.APP_DETAIL_MENU_ID)
            && !StringUtils.contains(menuId, Constant.APP_MENU_ID)) {
            menuId = menuId + StringPool.COMMA + Constant.APP_MENU_ID;
        }
        String[] menuIds = menuId.split(StringPool.COMMA);
        updateRoleMenus(role, menuIds);
    }

    private void updateRoleMenus(Role role, String[] menuIds) {
        List<RoleMenu> roleMenus = new ArrayList<>();
        for (String menuId : menuIds) {
            RoleMenu rm = new RoleMenu();
            rm.setMenuId(Long.valueOf(menuId));
            rm.setRoleId(role.getRoleId());
            roleMenus.add(rm);
        }
        roleMenuService.saveBatch(roleMenus);
    }
}
