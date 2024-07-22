/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.common.util

import org.apache.streampark.common.util.Implicits._

import org.apache.curator.RetryPolicy
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryNTimes
import org.apache.zookeeper.CreateMode

import java.nio.charset.StandardCharsets

import scala.collection.mutable

object ZooKeeperUtils {

  private[this] val connect = "localhost:2181"

  private[this] val map = mutable.Map[String, CuratorFramework]()

  def getClient(url: String = connect): CuratorFramework = {
    map.get(url) match {
      case Some(x) => x
      case None =>
        try {
          val retryPolicy: RetryPolicy = new RetryNTimes(5, 2000)
          val client = CuratorFrameworkFactory.builder
            .connectString(url)
            .retryPolicy(retryPolicy)
            .connectionTimeoutMs(2000)
            .build
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
      map -= url
    }
  }

  def listChildren(path: String, url: String = connect): List[String] = {
    val client = getClient(url)
    val stat = client.checkExists.forPath(path)
    stat match {
      case null => List.empty[String]
      case _ => client.getChildren.forPath(path).toList
    }
  }

  def create(
      path: String,
      value: String = null,
      url: String = connect,
      persistent: Boolean = false): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val data = value match {
            case null | "" => Array.empty[Byte]
            case _ => value.getBytes(StandardCharsets.UTF_8)
          }
          val mode = if (persistent) CreateMode.PERSISTENT else CreateMode.EPHEMERAL
          val opResult =
            client
              .create()
              .creatingParentsIfNeeded()
              .withMode(mode)
              .forPath(path, data)
          path == opResult
        case _ => false
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        false
    }
  }

  def update(
      path: String,
      value: String,
      url: String = connect,
      persistent: Boolean = false): Boolean = {
    try {
      val client = getClient(url)
      val stat = client.checkExists.forPath(path)
      stat match {
        case null =>
          val mode =
            if (persistent) CreateMode.PERSISTENT else CreateMode.EPHEMERAL
          val opResult = client.create.creatingParentsIfNeeded
            .withMode(mode)
            .forPath(path, value.getBytes(StandardCharsets.UTF_8))
          path == opResult
        case _ =>
          val opResult = client
            .setData()
            .forPath(path, value.getBytes(StandardCharsets.UTF_8))
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
