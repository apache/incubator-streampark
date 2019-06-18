package com.streamxhub.spark.monitor.system.dao;

import com.streamxhub.spark.monitor.system.domain.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    List<Role> findUserRole(String userName);

}
