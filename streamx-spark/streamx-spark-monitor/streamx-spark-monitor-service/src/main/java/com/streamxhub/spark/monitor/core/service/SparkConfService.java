package com.streamxhub.spark.monitor.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.spark.monitor.common.domain.QueryRequest;
import com.streamxhub.spark.monitor.core.domain.SparkConf;

import java.io.Serializable;
import java.util.Map;

/**
 * @author benjobs
 */
public interface SparkConfService extends IService<SparkConf> {
    /**
     * '
     *
     * @param sparkConf
     * @return
     */
    boolean config(SparkConf sparkConf);

    /**
     * @param sparkConf
     * @param request
     * @return
     */
    IPage<SparkConf> getPager(SparkConf sparkConf, QueryRequest request);

    /**
     * @param myId
     */
    Integer delete(String myId);

    /**
     * @param myId
     * @param conf
     */
    void update(String myId, String conf, Long userId);


    /**
     * @param myId
     * @param conf
     * @return
     */
    Map<String, Serializable> verify(String myId, String conf);
}
