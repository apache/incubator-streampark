package com.streamxhub.console.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.system.entity.RoleMenu;

import java.util.List;

public interface RoleMenuServie extends IService<RoleMenu> {

    void deleteRoleMenusByRoleId(String[] roleIds);

    void deleteRoleMenusByMenuId(String[] menuIds);

    List<RoleMenu> getRoleMenusByRoleId(String roleId);
}
