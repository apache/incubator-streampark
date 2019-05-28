package com.streamxhub.spark.core.util


import com.google.common.base.{Objects, Strings}
import org.apache.commons.compress.utils.Charsets
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.zookeeper.CreateMode

import scala.collection.JavaConverters._

object ZookeeperUtil {

  private[this] val defZkURL = "localhost:2181"

  private[this] val map: java.util.HashMap[String, CuratorFramework] = new java.util.HashMap[String, CuratorFramework]()

  private[this] def getClient(url: String = defZkURL): CuratorFramework = {
    if (map.get(url) == null) {
      try {
        val client = CuratorFrameworkFactory
          .builder
          .connectString(url)
          .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000))
          .connectionTimeoutMs(2000).build
        client.start()
        map.put(url, client)
      } catch {
        case e: Exception => throw new IllegalStateException(e.getMessage, e)
      }
    }
    map.get(url)
  }

  def destroy(url: String): Unit = {
    val client = getClient(url)
    if (client != null) {
      client.close()
    }
  }

  def listChildren(path: String, url: String = defZkURL): Array[String] = {
    val client = getClient(url)
    val stat = client.checkExists.forPath(path)
    stat match {
      case null =>
        val childrenBuilder = client.getChildren
        childrenBuilder.forPath(path).asScala.toArray
      case _ =>
        Array.empty[String]
    }
  }

  def create(path: String, value: String = null, url: String = defZkURL): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val data = if (Strings.isNullOrEmpty(value)) {
            Array.empty[Byte]
          } else {
            value.getBytes(Charsets.UTF_8)
          }
          val opResult = client.create.withMode(CreateMode.EPHEMERAL).forPath(path, data)
          Objects.equal(path, opResult)
        case _ => false
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    }
  }

  def update(path: String, value: String, url: String = defZkURL): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val opResult = client.create.creatingParentsIfNeeded.withMode(CreateMode.EPHEMERAL).forPath(path, value.getBytes(Charsets.UTF_8))
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

  def delete(path: String, url: String = defZkURL): Unit = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      if (stat != null) {
        client.delete.deletingChildrenIfNeeded.forPath(path)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def get(path: String, url: String = defZkURL): String = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val data = client.getData.forPath(path)
          new String(data)
        case _ => null
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        null
    }
  }

  def main(args: Array[String]): Unit = {
    ZookeeperUtil.create("/benjobs",
      """
        |{
        |"name":"benjobs",
        |"age":28,
        |"job":"spark"
        |}
      """.stripMargin)
    val data = ZookeeperUtil.get("/benjobs")
    println(data)
  }

}

