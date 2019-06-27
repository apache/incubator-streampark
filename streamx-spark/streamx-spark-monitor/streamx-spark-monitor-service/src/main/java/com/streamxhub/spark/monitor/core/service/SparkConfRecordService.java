package com.streamxhub.spark.monitor.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.spark.monitor.core.domain.SparkConfRecord;


/**
 * @author benjobs
 */
public interface SparkConfRecordService extends IService<SparkConfRecord> {
    void delete(String myId);
}
