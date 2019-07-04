package com.streamxhub.spark.monitor.core.service;

/**
 * @author benjobs
 */
public interface WatcherService {

    /**
     * @param id
     * @param conf
     */
    void config(String id, String conf);

    /**
     * @param id
     * @param conf
     */
    void publish(String id, String conf);

    /**
     * @param id
     * @param conf
     */
    void shutdown(String id, String conf);

    /**
     * @param myId
     */
    void delete(String myId);
}
