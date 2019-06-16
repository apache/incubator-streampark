package com.streamxhub.spark.monitor.api

import com.google.common.base.Objects
import org.apache.commons.compress.utils.Charsets
import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.zookeeper.CreateMode
import scala.collection.mutable
import scala.collection.JavaConverters._

object ZooKeeperUtil {

  private[this] val connect = "localhost:2181"

  private[this] val map = mutable.Map[String, CuratorFramework]()

  private[this] def getClient(url: String = connect): CuratorFramework = {
    map.get(url) match {
      case Some(x) => x
      case None =>
        try {
          // 设置重连策略ExponentialBackoffRetry, baseSleepTimeMs：初始sleep的时间,maxRetries：最大重试次数,maxSleepMs：最大重试时间
          //val retryPolicy = new ExponentialBackoffRetry(10000, 5)
          //（推荐）curator链接zookeeper的策略:RetryNTimes n：重试的次数 sleepMsBetweenRetries：每次重试间隔的时间
          //val retryPolicy:RetryPolicy = new RetryNTimes(3, 5000)
          // （不推荐） curator链接zookeeper的策略:RetryOneTime sleepMsBetweenRetry:每次重试间隔的时间,这个策略只会重试一次
          // val retryPolicy:RetryPolicy = new RetryOneTime(3000)
          // 永远重试，不推荐使用
          // val retryPolicy:RetryPolicy = new RetryForever(retryIntervalMs)
          // curator链接zookeeper的策略:RetryUntilElapsed maxElapsedTimeMs:最大重试时间 sleepMsBetweenRetries:每次重试间隔 重试时间超过maxElapsedTimeMs后，就不再重试
          // val retryPolicy:RetryPolicy = new RetryUntilElapsed(2000, 3000)
          val retryPolicy: RetryPolicy = new RetryNTimes(3, 2000)
          val client = CuratorFrameworkFactory
            .builder
            .connectString(url)
            .retryPolicy(retryPolicy)
            .connectionTimeoutMs(2000).build
          client.start()
          map += url -> client
          client
        } catch {
          case e: Exception => throw new IllegalStateException(e.getMessage, e)
        }
    }
  }

  def close(url: String): Unit = {
    val client = getClient(url)
    if (client != null) {
      client.close()
    }
  }

  def listChildren(path: String, url: String = connect): List[String] = {
    val client = getClient(url)
    val stat = client.checkExists.forPath(path)
    stat match {
      case null => List.empty[String]
      case _ => client.getChildren.forPath(path).asScala.toList
    }
  }

  def create(path: String, value: String = null, url: String = connect, persistent: Boolean = false): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val data = value match {
            case null | "" => Array.empty[Byte]
            case _ => value.getBytes(Charsets.UTF_8)
          }
          val mode = if (persistent) CreateMode.PERSISTENT else CreateMode.EPHEMERAL
          val opResult = client.create().creatingParentsIfNeeded().withMode(mode).forPath(path,data)
          Objects.equal(path, opResult)
        case _ => false
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    }
  }

  def update(path: String, value: String, url: String = connect, persistent: Boolean = false): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val mode = if (persistent) CreateMode.PERSISTENT else CreateMode.EPHEMERAL
          val opResult = client.create.creatingParentsIfNeeded.withMode(mode).forPath(path, value.getBytes(Charsets.UTF_8))
          Objects.equal(path, opResult)
        case _ =>
          val opResult = client.setData().forPath(path, value.getBytes(Charsets.UTF_8))
          opResult != null
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    }
  }

  def delete(path: String, url: String = connect): Unit = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
        case _ => client.delete.deletingChildrenIfNeeded().forPath(path)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def get(path: String, url: String = connect): String = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null => null
        case _ => new String(client.getData.forPath(path))
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        null
    }
  }

}
