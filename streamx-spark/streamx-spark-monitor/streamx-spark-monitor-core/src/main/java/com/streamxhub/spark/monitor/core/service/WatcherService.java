package com.streamxhub.spark.monitor.core.service;

public interface WatcherService {
    /**
     * 监测到配置文件发生注册或变动
     * @param currentPath
     * @param conf
     */
    void config(String currentPath, String conf);

    /**
     * 监测到服务发布上线
     * @param currentPath
     * @param conf
     */
    void publish(String currentPath, String conf);

    /**
     * 监测到服务停止
     * @param currentPath
     * @param conf
     */
    void shutdown(String currentPath, String conf);
}
