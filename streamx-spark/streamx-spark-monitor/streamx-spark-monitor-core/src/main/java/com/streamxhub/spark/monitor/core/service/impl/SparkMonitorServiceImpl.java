package com.streamxhub.spark.monitor.core.service.impl;

import com.streamxhub.spark.monitor.core.service.SparkMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class SparkMonitorServiceImpl implements SparkMonitorService {

    @Override
    public void publish(String id, Map<String, String> confMap) {

    }

    @Override
    public void shutdown(String id, Map<String, String> confMap) {

    }
}
