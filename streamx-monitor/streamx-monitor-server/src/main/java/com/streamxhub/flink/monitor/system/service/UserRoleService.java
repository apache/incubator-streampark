package com.streamxhub.flink.monitor.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.flink.monitor.system.entity.UserRole;

import java.util.List;

public interface UserRoleService extends IService<UserRole> {

    void deleteUserRolesByRoleId(String[] roleIds);

    void deleteUserRolesByUserId(String[] userIds);

    List<String> findUserIdsByRoleId(String[] roleIds);
}
