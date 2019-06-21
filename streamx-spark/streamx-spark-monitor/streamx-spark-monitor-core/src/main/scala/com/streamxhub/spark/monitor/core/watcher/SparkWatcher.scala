package com.streamxhub.spark.monitor.core.watcher

import java.io.StringReader

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.streamxhub.spark.monitor.api.Const._
import com.streamxhub.spark.monitor.api.util.{PropertiesUtil, ZooKeeperUtil}
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
import java.util.Properties
import scala.collection.JavaConversions._

import org.apache.curator.retry.ExponentialBackoffRetry

import scala.util.{Failure, Success, Try}


@Slf4j
@Component class SparkWatcher(@Value("${spark.app.monitor.zookeeper}") zookeeperConnect: String,
                              @Autowired watcherService: WatcherService) extends ApplicationRunner {

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
      watch(SPARK_CONF_PATH_PREFIX, (_: CuratorFramework, event: TreeCacheEvent) => {
        event.getData match {
          case null =>
          case data =>
            event.getType match {
              case NODE_ADDED | NODE_UPDATED =>
                val conf = new String(data.getData, StandardCharsets.UTF_8)
                val id = getId(data.getPath)
                val configMap = getConfigMap(conf)
                watcherService.config(id, configMap)
              case _ =>
            }
        }
      })
    })
    confThread.setDaemon(true)
    confThread.start()

    val monitorThread = factory.newThread(() => {
      watch(SPARK_MONITOR_PATH_PREFIX, (_: CuratorFramework, event: TreeCacheEvent) => {
        event.getData match {
          case null =>
          case data =>
            val path = data.getPath
            val conf = new String(data.getData, StandardCharsets.UTF_8)
            val id = getId(path)
            val configMap = getConfigMap(conf)
            event.getType match {
              case NODE_ADDED | NODE_UPDATED => watcherService.publish(id, configMap)
              case CONNECTION_LOST | NODE_REMOVED | INITIALIZED => watcherService.shutdown(id, configMap)
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
    Seq(SPARK_CONF_PATH_PREFIX, SPARK_MONITOR_PATH_PREFIX).foreach(ZooKeeperUtil.create(_, null, zookeeperConnect, persistent = true))
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

  private[this] def getConfigMap(conf: String): Map[String, String] = {
    if (!conf.matches(SPARK_CONF_REGEXP)) PropertiesUtil.getPropertiesFromYamlText(conf).toMap else {
      val properties = new Properties()
      properties.load(new StringReader(conf))
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    }
  }


}
