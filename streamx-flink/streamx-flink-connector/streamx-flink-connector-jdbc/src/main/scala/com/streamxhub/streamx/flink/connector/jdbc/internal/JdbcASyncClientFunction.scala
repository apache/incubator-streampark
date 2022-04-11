/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.jdbc.internal

import com.streamxhub.streamx.common.conf.ConfigConst.{KEY_ALIAS, KEY_SEMANTIC}
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.connector.jdbc.bean.HikariCPDataSourceProvider
import io.vertx.core.json.JsonObject
import io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME
import io.vertx.core.{AsyncResult, Handler, Vertx, VertxOptions}
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.{ResultSet, SQLClient, SQLConnection}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}

import java.util.{Collections, Properties}
import scala.collection.JavaConversions._

/**
 * 基于异步IO客户端实现
 *
 * @param sqlFun
 * @param resultFun
 * @param jdbc
 * @tparam T
 * @tparam R
 */

class JdbcASyncClientFunction[T: TypeInformation, R: TypeInformation](sqlFun: T => String, resultFunc: (T, Map[String, _]) => R, jdbc: Properties) extends RichAsyncFunction[T, R] with Logger {

  @transient private[this] var client: SQLClient = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    System.getProperties.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true")
    val clientConfig = new JsonObject()
    jdbc.foreach(x => clientConfig.put(x._1, x._2))
    clientConfig.remove(KEY_ALIAS)
    clientConfig.remove(KEY_SEMANTIC)
    //使用HikariCP连接池.
    clientConfig.put("provider_class", classOf[HikariCPDataSourceProvider].getName)
    val vertxOpts = new VertxOptions()
    val vertx = Vertx.vertx(vertxOpts)
    client = JDBCClient.createShared(vertx, clientConfig)
  }

  override def close(): Unit = {
    super.close()
    client.close()
  }

  @throws[Exception] def asyncInvoke(input: T, resultFuture: ResultFuture[R]): Unit = {
    client.getConnection(new Handler[AsyncResult[SQLConnection]]() {
      def handle(asyncResult: AsyncResult[SQLConnection]): Unit = {
        if (asyncResult.succeeded()) {
          asyncResult
            .result()
            .query(sqlFun(input), new Handler[AsyncResult[ResultSet]] {
              override def handle(event: AsyncResult[ResultSet]): Unit = {
                if (event.succeeded) {
                  val list = event.result().getRows()
                  if (list.isEmpty) {
                    resultFuture.complete(Collections.singleton(resultFunc(input, Map.empty[String, R])))
                  } else {
                    resultFuture.complete(list.map(x => resultFunc(input, x.getMap.toMap)))
                  }
                } else throw event.cause()
              }
            }).close()
        } else {
          throw asyncResult.cause()
        }
      }
    })
  }

  override def timeout(input: T, resultFuture: ResultFuture[R]): Unit = {
    logWarn("JdbcASyncClient request timeout. retrying... ")
    asyncInvoke(input, resultFuture)
  }

}
