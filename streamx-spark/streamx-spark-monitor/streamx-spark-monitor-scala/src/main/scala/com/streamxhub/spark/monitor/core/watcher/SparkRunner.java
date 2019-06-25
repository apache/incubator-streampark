package com.streamxhub.spark.monitor.core.watcher;

import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil;
import com.streamxhub.spark.monitor.core.service.WatcherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static com.streamxhub.spark.monitor.api.Const.*;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_UPDATED;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class SparkRunner implements ApplicationRunner {

    @Value("${spark.app.monitor.zookeeper}")
    private String zookeeperConnect;

    private CuratorFramework client;

    @Autowired
    private WatcherService watcherService;

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
        watch(SPARK_CONF_PATH_PREFIX(), (client, event) -> {
            ChildData data = event.getData();
            if (data != null && !data.getPath().equals(SPARK_CONF_PATH_PREFIX())) {
                if (event.getType().equals(NODE_ADDED) || event.getType().equals(NODE_UPDATED)) {
                    String conf = new String(data.getData(), StandardCharsets.UTF_8);
                    String id = getId(data.getPath());
                    watcherService.config(id, conf);
                }
            }
        });

        watch(SPARK_MONITOR_PATH_PREFIX(), (client, event) -> {
            ChildData data = event.getData();
            if (data != null && !data.getPath().equals(SPARK_MONITOR_PATH_PREFIX())) {
                String id = getId(data.getPath());
                String conf = new String(data.getData(), StandardCharsets.UTF_8);
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_UPDATED:
                        watcherService.publish(id, conf);
                        break;
                    case CONNECTION_LOST:
                    case NODE_REMOVED:
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
        try (TreeCache treeCache = new TreeCache(client, path)) {
            treeCache.getListenable().addListener(listener);
            treeCache.start();
        } catch (Exception ignored) {
        }
    }

    private String getId(String path) {
        return path.replaceAll("^/(.*)/", "");
    }

}
