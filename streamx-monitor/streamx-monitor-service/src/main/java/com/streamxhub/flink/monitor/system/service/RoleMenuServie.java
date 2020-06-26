package com.streamxhub.flink.monitor.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.flink.monitor.system.entity.RoleMenu;

import java.util.List;

public interface RoleMenuServie extends IService<RoleMenu> {

    void deleteRoleMenusByRoleId(String[] roleIds);

    void deleteRoleMenusByMenuId(String[] menuIds);

    List<RoleMenu> getRoleMenusByRoleId(String roleId);
}
