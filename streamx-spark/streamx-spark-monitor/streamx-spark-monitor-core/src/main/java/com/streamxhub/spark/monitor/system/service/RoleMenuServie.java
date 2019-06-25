package com.streamxhub.spark.monitor.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.spark.monitor.system.domain.RoleMenu;

import java.util.List;

public interface RoleMenuServie extends IService<RoleMenu> {

    void deleteRoleMenusByRoleId(String[] roleIds);

    void deleteRoleMenusByMenuId(String[] menuIds);

    List<RoleMenu> getRoleMenusByRoleId(String roleId);
}
