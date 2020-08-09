package com.streamxhub.flink.monitor.core.dao;

import com.streamxhub.flink.monitor.core.entity.Application;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface ApplicationMapper extends BaseMapper<Application> {
    IPage<Application> findApplication(Page<Application> page,@Param("application") Application application);

    void updateDeploy(@Param("application") Application application);

}
