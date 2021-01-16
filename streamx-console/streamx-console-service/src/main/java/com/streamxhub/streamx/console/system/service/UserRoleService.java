package com.streamxhub.streamx.console.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.streamx.console.system.entity.UserRole;

public interface UserRoleService extends IService<UserRole> {

    void deleteUserRolesByRoleId(String[] roleIds);

    void deleteUserRolesByUserId(String[] userIds);

    List<String> findUserIdsByRoleId(String[] roleIds);
}
