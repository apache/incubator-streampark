package com.streamxhub.spark.monitor.core.watcher

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.streamxhub.spark.monitor.api.Const
import com.streamxhub.spark.monitor.api.util.ZooKeeperUtil
import com.streamxhub.spark.monitor.core.service.WatcherService
import lombok.extern.slf4j.Slf4j
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{TreeCache, TreeCacheEvent, TreeCacheListener}
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type._
import org.apache.curator.framework.CuratorFrameworkFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.nio.charset.StandardCharsets

import scala.collection.JavaConversions._
import org.apache.curator.retry.ExponentialBackoffRetry

import scala.util.{Failure, Success, Try}


@Slf4j
@Component class SparkWatcher(@Autowired watcherService: WatcherService) extends ApplicationRunner {

  @Value("${spark.app.monitor.zookeeper}") private val zookeeperConnect: String = ""

  /**
    * 会话超时时间
    */
  private val SESSION_TIMEOUT = 30000

  /**
    * 连接超时时间
    */
  private val CONNECTION_TIMEOUT = 5000
  /**
    * 创建连接实例
    */
  private var client: CuratorFramework = _

  private val factory = new ThreadFactoryBuilder().setNameFormat("pull-thread-%d").build

  private def getId(path: String) = path.replaceAll("^/(.*)/", "")

  def run(args: ApplicationArguments): Unit = {

    val confThread = factory.newThread(() => {
      watch(Const.SPARK_CONF_PATH_PREFIX, (_: CuratorFramework, event: TreeCacheEvent) => {
        event.getData match {
          case null =>
          case data =>
            event.getType match {
              case NODE_ADDED | NODE_UPDATED =>
                val conf = new String(data.getData, StandardCharsets.UTF_8)
                watcherService.config(getId(data.getPath), conf)
              case _ =>
            }
        }
      })
    })
    confThread.setDaemon(true)
    confThread.start()

    val monitorThread = factory.newThread(() => {
      watch(Const.SPARK_MONITOR_PATH_PREFIX, (_: CuratorFramework, event: TreeCacheEvent) => {
        event.getData match {
          case null =>
          case data =>
            val path = data.getPath
            val conf = new String(data.getData, StandardCharsets.UTF_8)
            event.getType match {
              case NODE_ADDED | NODE_UPDATED => watcherService.publish(getId(path), conf)
              case CONNECTION_LOST | NODE_REMOVED | INITIALIZED => watcherService.shutdown(getId(path), conf)
              case _ =>
            }
        }
      })
    })
    monitorThread.setDaemon(true)
    monitorThread.start()

  }

  @PostConstruct def initialize(): Unit = {
    //检查监控路径是否存在,不存在在创建...
    Seq(Const.SPARK_CONF_PATH_PREFIX, Const.SPARK_MONITOR_PATH_PREFIX).foreach(ZooKeeperUtil.create(_, null, zookeeperConnect, persistent = true))
    //获取连接实例...
    client = CuratorFrameworkFactory.newClient(zookeeperConnect, SESSION_TIMEOUT, CONNECTION_TIMEOUT, new ExponentialBackoffRetry(1000, 3))
    client.start()
  }

  @PreDestroy def destroy(): Unit = client.close()

  private def watch(parent: String, listener: TreeCacheListener): Unit = {
    Try {
      client.getChildren.forPath(parent).filter(_.nonEmpty).foreach(x => {
        //监听当前节点
        val treeCache = new TreeCache(client, s"$parent/$x")
        //设置监听器和处理过程
        treeCache.getListenable.addListener(listener)
        //开始监听
        treeCache.start
      })
    } match {
      case Failure(e) => e.printStackTrace()
      case Success(_) =>
    }
  }

}
