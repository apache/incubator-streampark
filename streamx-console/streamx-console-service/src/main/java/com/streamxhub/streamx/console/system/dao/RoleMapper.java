package com.streamxhub.streamx.console.system.dao;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.streamxhub.streamx.console.system.entity.Role;

public interface RoleMapper extends BaseMapper<Role> {

    List<Role> findUserRole(String userName);
}
