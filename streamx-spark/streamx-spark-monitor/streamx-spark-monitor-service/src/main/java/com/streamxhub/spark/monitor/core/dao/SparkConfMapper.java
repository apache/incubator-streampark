package com.streamxhub.spark.monitor.core.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.streamxhub.spark.monitor.core.domain.SparkConf;

/**
 * @author benjobs
 */
public interface SparkConfMapper extends BaseMapper<SparkConf> {
    IPage<SparkConf> getConf(Page<SparkConf> page, SparkConf sparkConf);
}
