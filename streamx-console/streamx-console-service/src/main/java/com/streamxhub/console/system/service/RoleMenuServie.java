package com.streamxhub.console.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.system.entity.RoleMenu;

public interface RoleMenuServie extends IService<RoleMenu> {

    void deleteRoleMenusByRoleId(String[] roleIds);

    void deleteRoleMenusByMenuId(String[] menuIds);

    List<RoleMenu> getRoleMenusByRoleId(String roleId);
}
