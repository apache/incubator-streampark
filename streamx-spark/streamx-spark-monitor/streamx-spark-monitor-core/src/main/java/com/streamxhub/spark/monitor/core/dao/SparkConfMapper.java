package com.streamxhub.spark.monitor.core.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.streamxhub.spark.monitor.core.domain.SparkConf;

/**
 * @author benjobs
 */
public interface SparkConfMapper extends BaseMapper<SparkConf> {
    void saveRecord(SparkConf sparkConf);
}
