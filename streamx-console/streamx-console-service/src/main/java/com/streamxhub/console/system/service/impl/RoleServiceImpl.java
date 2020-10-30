package com.streamxhub.console.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.system.dao.RoleMapper;
import com.streamxhub.console.system.dao.RoleMenuMapper;
import com.streamxhub.console.system.entity.Role;
import com.streamxhub.console.system.entity.RoleMenu;
import com.streamxhub.console.system.service.RoleMenuServie;
import com.streamxhub.console.system.service.RoleService;
import com.streamxhub.console.system.service.UserRoleService;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.base.utils.SortUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleMenuServie roleMenuService;

    @Override
    public Set<String> getUserRoleName(String username) {
        List<Role> roleList = this.findUserRole(username);
        return roleList.stream().map(Role::getRoleName).collect(Collectors.toSet());
    }

    @Override
    public IPage<Role> findRoles(Role role, RestRequest request) {
        try {
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();

            if (StringUtils.isNotBlank(role.getRoleName())) {
                queryWrapper.eq(Role::getRoleName, role.getRoleName());
            }
            if (StringUtils.isNotBlank(role.getCreateTimeFrom()) && StringUtils.isNotBlank(role.getCreateTimeTo())) {
                queryWrapper
                        .ge(Role::getCreateTime, role.getCreateTimeFrom())
                        .le(Role::getCreateTime, role.getCreateTimeTo());
            }
            Page<Role> page = new Page<>();
            SortUtil.handlePageSort(request, page, true);
            return this.page(page, queryWrapper);
        } catch (Exception e) {
            log.info("获取角色信息失败", e);
            return null;
        }
    }

    @Override
    public List<Role> findUserRole(String userName) {
        return baseMapper.findUserRole(userName);
    }

    @Override
    public Role findByName(String roleName) {
        return baseMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, roleName));
    }

    @Override
    public void createRole(Role role) {
        role.setCreateTime(new Date());
        this.save(role);

        String[] menuIds = role.getMenuId().split(StringPool.COMMA);
        setRoleMenus(role, menuIds);
    }

    @Override
    public void deleteRoles(String[] roleIds) throws Exception {
        List<String> list = Arrays.asList(roleIds);
        baseMapper.deleteBatchIds(list);
        this.roleMenuService.deleteRoleMenusByRoleId(roleIds);
        this.userRoleService.deleteUserRolesByRoleId(roleIds);
    }

    @Override
    public void updateRole(Role role) throws Exception {
        // 查找这些角色关联了那些用户
        String[] roleId = {String.valueOf(role.getRoleId())};
        role.setModifyTime(new Date());
        baseMapper.updateById(role);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, role.getRoleId()));
        String[] menuIds = role.getMenuId().split(StringPool.COMMA);
        setRoleMenus(role, menuIds);
    }

    private void setRoleMenus(Role role, String[] menuIds) {
        Arrays.stream(menuIds).forEach(menuId -> {
            RoleMenu rm = new RoleMenu();
            rm.setMenuId(Long.valueOf(menuId));
            rm.setRoleId(role.getRoleId());
            this.roleMenuMapper.insert(rm);
        });
    }
}
