package com.streamxhub.console.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.system.entity.Role;

import java.util.List;
import java.util.Set;

public interface RoleService extends IService<Role> {

    Set<String> getUserRoleName(String username);

    IPage<Role> findRoles(Role role, RestRequest request);

    List<Role> findUserRole(String userName);

    Role findByName(String roleName);

    void createRole(Role role);

    void deleteRoles(String[] roleIds) throws Exception;

    void updateRole(Role role) throws Exception;
}
