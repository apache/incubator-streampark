package com.streamxhub.spark.monitor.core.watcher;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.streamxhub.spark.monitor.api.Const;
import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author benjobs
 */
@Slf4j
@Component
public class SparkWatcher implements ApplicationRunner {

    @Value("${spark.app.monitor.zookeeper}")
    private String zookeeperConnect;

    /**
     * 会话超时时间
     */
    private static final int SESSION_TIMEOUT = 30000;

    /**
     * 连接超时时间
     */
    private static final int CONNECTION_TIMEOUT = 5000;
    /**
     * 创建连接实例
     */
    private CuratorFramework client = null;

    private static final String sparkConfPath = Const.SPARK_CONF_PATH_PREFIX();

    private static final String sparkMonitorPath = Const.SPARK_MONITOR_PATH_PREFIX();

    private ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("pull-thread-%d").build();

    @Autowired
    private WatcherService watcherService;

    @Override
    public void run(ApplicationArguments args) {

        factory.newThread(() -> pull(sparkConfPath, (client, event) -> {
            ChildData data = event.getData();
            if (data != null) {
                String currentPath = data.getPath();
                String conf = new String(data.getData(), StandardCharsets.UTF_8);
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_UPDATED:
                        watcherService.config(currentPath, conf);
                        break;
                    default:
                        break;
                }
            }
        })).start();

        factory.newThread(() -> pull(sparkMonitorPath, (client, event) -> {
            ChildData data = event.getData();
            if (data != null) {
                String currentPath = data.getPath();
                String conf = new String(data.getData(), StandardCharsets.UTF_8);
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_UPDATED:
                        watcherService.publish(currentPath, conf);
                        break;
                    case CONNECTION_LOST:
                    case NODE_REMOVED:
                    case INITIALIZED:
                    case CONNECTION_RECONNECTED:
                        watcherService.shutdown(currentPath, conf);
                        break;
                    default:
                        break;
                }
            }
        })).start();
    }

    @PostConstruct
    public void initialize() {
        //检查监控路径是否存在,不存在在创建...
        checkPathIfNotExists();
        //获取zk连接实例
        client = CuratorFrameworkFactory.newClient(zookeeperConnect,
                SESSION_TIMEOUT,
                CONNECTION_TIMEOUT,
                new ExponentialBackoffRetry(1000, 3)
        );
        //启动
        client.start();
    }

    @PreDestroy
    public void destory() {
        client.close();
    }

    private void pull(String parentPath, TreeCacheListener listener) {
        try {
            List<String> paths = client.getChildren().forPath(parentPath);
            if (paths != null && !paths.isEmpty()) {
                paths.forEach(path -> {
                    try {
                        String confPath = parentPath + "/" + path;
                        //监听当前节点
                        TreeCache treeCache = new TreeCache(client, confPath);
                        //设置监听器和处理过程
                        treeCache.getListenable().addListener(listener);
                        //开始监听
                        treeCache.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPathIfNotExists() {
        ZooKeeperUtil.create(sparkConfPath, null, zookeeperConnect, true);
        ZooKeeperUtil.create(sparkMonitorPath, null, zookeeperConnect, true);
    }

}
