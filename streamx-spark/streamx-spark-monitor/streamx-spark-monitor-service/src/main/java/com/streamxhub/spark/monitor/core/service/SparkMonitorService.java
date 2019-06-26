package com.streamxhub.spark.monitor.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;

import java.util.Map;

public interface SparkMonitorService {
    void publish(String id, Map<String, String> confMap);

    void shutdown(String id, Map<String, String> confMap);

    IPage<SparkMonitor> getMonitor(SparkMonitor sparkMonitor,QueryRequest request);

}
