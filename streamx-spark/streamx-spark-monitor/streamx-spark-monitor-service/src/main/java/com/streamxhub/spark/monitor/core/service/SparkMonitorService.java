package com.streamxhub.spark.monitor.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.core.domain.SparkMonitor;

import java.util.Map;

/**
 * @author benjobs
 */
public interface SparkMonitorService {

    void publish(String id, Map<String, String> confMap);

    void shutdown(String id, Map<String, String> confMap);

    IPage<SparkMonitor> getPager(SparkMonitor sparkMonitor,QueryRequest request);

    void delete(String myId);

    int start(String myId);

    int stop(String myId);

    void checkStart(String myId);

    void checkStop(String myId);

}
