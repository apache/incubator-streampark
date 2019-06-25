package com.streamxhub.spark.monitor.core.service;

import java.util.Map;

public interface SparkMonitorService {
    void publish(String id, Map<String, String> confMap);

    void shutdown(String id, Map<String, String> confMap);
}
