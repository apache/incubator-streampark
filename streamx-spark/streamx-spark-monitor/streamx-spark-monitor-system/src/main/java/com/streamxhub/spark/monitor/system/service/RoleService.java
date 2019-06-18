package com.streamxhub.spark.monitor.system.service;

import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.system.domain.Role;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RoleService extends IService<Role> {

    IPage<Role> findRoles(Role role, QueryRequest request);

    List<Role> findUserRole(String userName);

    Role findByName(String roleName);

    void createRole(Role role);

    void deleteRoles(String[] roleIds) throws Exception;

    void updateRole(Role role) throws Exception;
}
