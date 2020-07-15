/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.core.request

import java.util.Properties
import java.util.concurrent.TimeUnit

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{AsyncDataStream, DataStream}
import io.vertx.core.{AsyncResult, Handler, ServiceHelper, Vertx, VertxOptions}
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.ResultSet
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLClient
import io.vertx.ext.sql.SQLConnection
import java.util.Collections

import io.vertx.core.spi.VertxFactory

import scala.annotation.meta.param

object MySQLRequest {

  def apply[T: TypeInformation](@(transient@param) stream: DataStream[T], overrideParams: Map[String, String] = Map.empty[String, String]): MySQLRequest[T] = new MySQLRequest[T](stream, overrideParams)

}

class MySQLRequest[T: TypeInformation](@(transient@param) private val stream: DataStream[T], overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param sqlFun
   * @param jdbc
   * @tparam R
   * @return
   */
  def requestOrdered[R: TypeInformation](sqlFun: T => String, resultFun: java.util.Map[String, _] => R, timeout: Long = 1000, capacity: Int = 10)(implicit jdbc: Properties): DataStream[R] = {
    val async = new MySQLASyncIOFunction[T, R](sqlFun, resultFun, jdbc)
    AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

  def requestUnordered[R: TypeInformation](sqlFun: T => String, resultFun: java.util.Map[String, _] => R, timeout: Long = 1000, capacity: Int = 10)(implicit jdbc: Properties): DataStream[R] = {
    val async = new MySQLASyncIOFunction[T, R](sqlFun, resultFun, jdbc)
    AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

}


class MySQLASyncIOFunction[T: TypeInformation, R: TypeInformation](sqlFun: T => String, resultFun: java.util.Map[String, _] => R, jdbc: Properties) extends RichAsyncFunction[T, R] {
  private var client: SQLClient = null

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    val clientConfig = new JsonObject()
    jdbc.foreach(x => clientConfig.put(x._1, x._2))
    //HikariCP连接池.
    clientConfig.put("provider_class","io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
    val vertxOpts = new VertxOptions()
    val factory = ServiceHelper.loadFactory(classOf[VertxFactory])
    val vertx = factory.vertx(vertxOpts)
    client = JDBCClient.createNonShared(vertx, clientConfig)
  }

  override def close(): Unit = {
    super.close()
    client.close()
  }

  @throws[Exception]
  def asyncInvoke(input: T, resultFuture: ResultFuture[R]): Unit = {
    client.getConnection(new Handler[AsyncResult[SQLConnection]]() {
      def handle(asyncResult: AsyncResult[SQLConnection]): Unit = {
        if (!asyncResult.failed) {
          val connection = asyncResult.result()
          connection.query(sqlFun(input), new Handler[AsyncResult[ResultSet]] {
            override def handle(event: AsyncResult[ResultSet]): Unit = {
              if (event.succeeded) {
                event.result().getRows().foreach(x => {
                  resultFuture.complete(Collections.singleton(resultFun(x.getMap.asScala)))
                })
              }
            }
          })
        }
      }
    })
  }

}
