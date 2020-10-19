package com.streamxhub.console.core.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.streamxhub.console.core.entity.Application;
import com.streamxhub.console.core.entity.ApplicationLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author benjobs
 */
public interface ApplicationLogMapper extends BaseMapper<ApplicationLog> {

    @Select("SELECT * from t_flink_log where app_id=#{applicationLog.appId}")
    IPage<ApplicationLog> page(Page<Application> page, @Param("applicationLog")ApplicationLog log);
}
