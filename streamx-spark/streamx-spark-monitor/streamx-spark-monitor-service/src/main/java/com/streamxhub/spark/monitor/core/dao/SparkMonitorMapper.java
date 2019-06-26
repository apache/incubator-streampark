package com.streamxhub.spark.monitor.core.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;
import org.apache.ibatis.annotations.Param;

/**
 * @author benjobs
 */
public interface SparkMonitorMapper extends BaseMapper<SparkMonitor> {

    IPage<SparkMonitor> getMonitor(Page<SparkMonitor> page,@Param("sparkMonitor") SparkMonitor sparkMonitor);

}
