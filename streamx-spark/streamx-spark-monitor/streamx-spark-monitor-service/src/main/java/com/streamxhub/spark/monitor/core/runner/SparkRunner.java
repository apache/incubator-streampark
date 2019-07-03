package com.streamxhub.spark.monitor.core.runner;

import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.*;
import java.nio.charset.StandardCharsets;

import static com.streamxhub.spark.monitor.api.Const.*;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.*;

@Slf4j
@Component
public class SparkRunner implements ApplicationRunner {

    @Value("${spark.app.monitor.zookeeper}")
    private String zookeeperConnect;

    private CuratorFramework client;

    @Autowired
    private WatcherService watcherService;

    private String zkConfPath = SPARK_CONF_PATH_PREFIX();

    private String zkHBPath = SPARK_MONITOR_PATH_PREFIX();


    @PostConstruct
    public void initialize() {
        client = ZooKeeperUtil.getClient(zookeeperConnect);
    }

    @PreDestroy
    public void destroy() {
        ZooKeeperUtil.close(zookeeperConnect);
    }

    @Override
    public void run(ApplicationArguments args) {

        watch(zkConfPath, (client, event) -> {
            ChildData data = event.getData();
            if (data != null && !data.getPath().equals(zkConfPath)) {
                if (event.getType().equals(NODE_ADDED) || event.getType().equals(NODE_UPDATED)) {
                    String conf = new String(data.getData(), StandardCharsets.UTF_8);
                    String id = getId(data.getPath());
                    watcherService.config(id, conf);
                }
            }
        });

        watch(zkHBPath, (client, event) -> {
            ChildData data = event.getData();
            if (data != null && !data.getPath().equals(zkHBPath)) {
                String id = getId(data.getPath());
                String conf = new String(data.getData(), StandardCharsets.UTF_8);
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_UPDATED:
                    case CONNECTION_RECONNECTED:
                        watcherService.publish(id, conf);
                        break;
                    case NODE_REMOVED:
                    case CONNECTION_LOST:
                    case INITIALIZED:
                        watcherService.shutdown(id, conf);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void watch(String path, TreeCacheListener listener) {
        try {
            TreeCache treeCache = new TreeCache(client, path);
            treeCache.getListenable().addListener(listener);
            treeCache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getId(String path) {
        return path.replaceAll("^/(.*)/", "");
    }

}
