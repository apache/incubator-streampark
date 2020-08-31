package com.streamxhub.monitor.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.monitor.base.domain.RestRequest;
import com.streamxhub.monitor.system.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

    IPage<Role> findRoles(Role role, RestRequest request);

    List<Role> findUserRole(String userName);

    Role findByName(String roleName);

    void createRole(Role role);

    void deleteRoles(String[] roleIds) throws Exception;

    void updateRole(Role role) throws Exception;
}
