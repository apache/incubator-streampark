package com.streamxhub.spark.core.util


import com.google.common.base.{Objects, Strings}
import org.apache.commons.compress.utils.Charsets
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.zookeeper.CreateMode

import scala.collection.JavaConverters._
import scala.collection.mutable

object ZookeeperUtil {

  private[this] val defZkURL = "localhost:2181"

  private[this] val map = mutable.Map[String, CuratorFramework]()

  private[this] def getClient(url: String = defZkURL): CuratorFramework = {
    map.get(url) match {
      case Some(x) => x
      case None =>
        try {
          val client = CuratorFrameworkFactory
            .builder
            .connectString(url)
            .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 1000))
            .connectionTimeoutMs(2000).build
          client.start()
          map += url -> client
          client
        } catch {
          case e: Exception => throw new IllegalStateException(e.getMessage, e)
        }
    }
  }

  def destroy(url: String): Unit = {
    val client = getClient(url)
    if (client != null) {
      client.close()
    }
  }

  def listChildren(path: String, url: String = defZkURL): List[String] = {
    val client = getClient(url)
    val stat = client.checkExists.forPath(path)
    stat match {
      case null => client.getChildren.forPath(path).asScala.toList
      case _ => List.empty[String]
    }
  }

  def create(path: String, value: String = null, url: String = defZkURL): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val data = value match {
            case null | "" => Array.empty[Byte]
            case _ => value.getBytes(Charsets.UTF_8)
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
        case null => null
        case _ => new String(client.getData.forPath(path))
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

