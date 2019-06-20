package com.streamxhub.spark.monitor.core.watcher;


import com.streamxhub.spark.monitor.api.Const;
import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    final Map<String, String> monitor = new ConcurrentHashMap<>();

    final Map<String, String> sparkConf = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String sparkConfPath = Const.SPARK_CONF_PATH_PREFIX();
        String sparkMonitorPath = Const.SPARK_MONITOR_PATH_PREFIX();

        initialize(sparkConfPath, sparkMonitorPath);

        pull(sparkConfPath, (client, event) -> {
            ChildData data = event.getData();
            if (data != null) {
                String currentPath = data.getPath();
                String conf = new String(data.getData(), StandardCharsets.UTF_8);
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_UPDATED:
                        sparkConf.put(currentPath, conf);
                        break;
                    default:
                        break;
                }
            }
        });

        pull(sparkMonitorPath, (client, event) -> {
            ChildData data = event.getData();
            if (data != null) {
                String currentPath = data.getPath();
                String conf = new String(data.getData(), StandardCharsets.UTF_8);
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_UPDATED:
                        monitor.put(currentPath, conf);
                        break;
                    case CONNECTION_LOST:
                    case NODE_REMOVED:
                    case INITIALIZED:
                    case CONNECTION_RECONNECTED:
                        monitor.remove(currentPath);
                        break;
                    default:
                        break;
                }
            }
        });
    }


    @PostConstruct
    public void init() {
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

    private void pull(String parentPath, TreeCacheListener listener) throws Exception {
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
    }

    private void initialize(String sparkConfPath, String sparkMonitorPath) {
        ZooKeeperUtil.create(sparkConfPath,null,zookeeperConnect,true);
        ZooKeeperUtil.create(sparkMonitorPath,null,zookeeperConnect,true);
    }


}
