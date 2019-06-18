package com.streamxhub.spark.monitor.system.dao;

import com.streamxhub.spark.monitor.system.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    IPage<User> findUserDetail(Page page, @Param("user") User user);
}
