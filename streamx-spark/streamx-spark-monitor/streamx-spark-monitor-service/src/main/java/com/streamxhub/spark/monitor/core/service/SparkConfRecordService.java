package com.streamxhub.spark.monitor.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.spark.monitor.core.domain.SparkConf;
import com.streamxhub.spark.monitor.core.domain.SparkConfRecord;

import java.util.List;


/**
 * @author benjobs
 */
public interface SparkConfRecordService extends IService<SparkConfRecord> {
    void delete(String myId);

    List<SparkConfRecord> getRecords(String myId);

    void addRecord(SparkConf existConf);
}
