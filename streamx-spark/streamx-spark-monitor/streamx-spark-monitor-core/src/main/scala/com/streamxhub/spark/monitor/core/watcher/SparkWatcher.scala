package com.streamxhub.spark.monitor.core.watcher


import com.streamxhub.spark.monitor.api.Const._
import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil
import com.streamxhub.spark.monitor.core.service.WatcherService
import lombok.extern.slf4j.Slf4j
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{TreeCache, TreeCacheEvent, TreeCacheListener}
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success, Try}

@Slf4j
@Component
class SparkWatcher(@Value("${spark.app.monitor.zookeeper}") zookeeperConnect: String,
                   @Autowired watcherService: WatcherService) extends ApplicationRunner {
  /**
    * 创建连接实例
    */
  private val client: CuratorFramework = ZooKeeperUtil.getClient(zookeeperConnect)

  @PostConstruct def initialize(): Unit = {

  }

  @PreDestroy def destroy(): Unit = ZooKeeperUtil.close(zookeeperConnect)

  @Override def run(args: ApplicationArguments): Unit = {
    watch(SPARK_CONF_PATH_PREFIX, (_: CuratorFramework, event: TreeCacheEvent) => {
      event.getData match {
        case null =>
        case data if data.getPath != SPARK_CONF_PATH_PREFIX =>
          event.getType match {
            case NODE_ADDED | NODE_UPDATED =>
              val conf = new String(data.getData, StandardCharsets.UTF_8)
              val id = getId(data.getPath)
              watcherService.config(id, conf)
            case _ =>
          }
        case _ =>
      }
    })

    watch(SPARK_MONITOR_PATH_PREFIX, (_: CuratorFramework, event: TreeCacheEvent) => {
      event.getData match {
        case null =>
        case data if data.getPath != SPARK_MONITOR_PATH_PREFIX =>
          val id = getId(data.getPath)
          val conf = new String(data.getData, StandardCharsets.UTF_8)
          event.getType match {
            case NODE_ADDED | NODE_UPDATED => watcherService.publish(id, conf)
            case CONNECTION_LOST | NODE_REMOVED | INITIALIZED => watcherService.shutdown(id, conf)
            case _ =>
          }
        case _ =>
      }
    })
  }

  private[this] def watch(path: String, listener: TreeCacheListener): Unit = {
    Try {
      //监听当前节点
      val treeCache = new TreeCache(client, path)
      //设置监听器和处理过程
      treeCache.getListenable.addListener(listener)
      //开始监听
      treeCache.start
    } match {
      case Failure(e) => e.printStackTrace()
      case Success(_) =>
    }
  }

  private[this] def getId(path: String): String = path.replaceAll("^/(.*)/", "")


}
