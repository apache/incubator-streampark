package com.streamxhub.spark.monitor.core.service;

/**
 * @author benjobs
 */
public interface WatcherService {

    void config(String id, String conf);

    void publish(String id, String conf);

    void shutdown(String id, String conf);

    void delete(String myId);
}
