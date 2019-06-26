package com.streamxhub.spark.monitor.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.core.domain.SparkConf;

public interface SparkConfService extends IService<SparkConf> {
    boolean config(SparkConf sparkConf);
    IPage<SparkConf> getConf(SparkConf sparkConf, QueryRequest request);
}
