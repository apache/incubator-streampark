package com.streamxhub.flink.monitor.system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.streamxhub.flink.monitor.system.entity.Role;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    List<Role> findUserRole(String userName);

}
