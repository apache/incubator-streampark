package com.streamxhub.spark.monitor.system.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.streamxhub.spark.monitor.system.domain.Role;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    List<Role> findUserRole(String userName);

}
