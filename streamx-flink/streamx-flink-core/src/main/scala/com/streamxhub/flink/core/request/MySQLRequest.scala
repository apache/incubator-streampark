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
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, TimeUnit}

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.{AsyncDataStream, DataStream}
import io.vertx.core.{AsyncResult, Handler, Vertx, VertxOptions}
import io.vertx.core.json.JsonObject
import io.vertx.ext.sql.ResultSet
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.async.{ResultFuture, RichAsyncFunction}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.SQLClient
import io.vertx.ext.sql.SQLConnection
import io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME
import java.util.Collections
import java.util.function.{Consumer, Supplier}

import com.streamxhub.common.conf.ConfigConst.KEY_INSTANCE
import com.streamxhub.common.util.{JdbcUtils, Logger}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource

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
  def requestOrdered[R: TypeInformation](@(transient@param) sqlFun: T => String, @(transient@param) resultFun: Map[String, _] => R, timeout: Long = 1000, capacity: Int = 10)(implicit jdbc: Properties): DataStream[R] = {
    val async = new MySQLASyncClientFunction[T, R](sqlFun, resultFun, jdbc)
    AsyncDataStream.orderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

  def requestUnordered[R: TypeInformation](@(transient@param) sqlFun: T => String, @(transient@param) resultFun: Map[String, _] => R, timeout: Long = 1000, capacity: Int = 10)(implicit jdbc: Properties): DataStream[R] = {
    val async = new MySQLASyncClientFunction[T, R](sqlFun, resultFun, jdbc)
    AsyncDataStream.unorderedWait(stream, async, timeout, TimeUnit.MILLISECONDS, capacity)
  }

}

/**
 * 基于异步IO客户端实现
 *
 * @param sqlFun
 * @param resultFun
 * @param jdbc
 * @tparam T
 * @tparam R
 */

class MySQLASyncClientFunction[T: TypeInformation, R: TypeInformation](sqlFun: T => String, resultFun: Map[String, _] => R, jdbc: Properties) extends RichAsyncFunction[T, R] with Logger {
  @transient private[this] var client: SQLClient = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    System.getProperties().setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true")
    val clientConfig = new JsonObject()
    jdbc.foreach(x => clientConfig.put(x._1, x._2))
    clientConfig.remove(KEY_INSTANCE)
    //使用HikariCP连接池.
    clientConfig.put("provider_class", classOf[HikariCPDataSourceProvider].getName)
    val vertxOpts = new VertxOptions()
    val vertx = Vertx.vertx(vertxOpts)
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
        if (asyncResult.succeeded()) {
          asyncResult
            .result()
            .query(sqlFun(input), new Handler[AsyncResult[ResultSet]] {
              override def handle(event: AsyncResult[ResultSet]): Unit = {
                if (event.succeeded) {
                  event.result().getRows().foreach(x => {
                    resultFuture.complete(Collections.singleton(resultFun(x.getMap.asScala.toMap)))
                  })
                } else {
                  throw event.cause()
                }
              }
            }).close()
        } else {
          throw asyncResult.cause()
        }
      }
    })
  }

  override def timeout(input: T, resultFuture: ResultFuture[R]): Unit = {
    logger.warn("[Streamx] MySQLASyncClient request timeout. retrying... ")
    asyncInvoke(input, resultFuture)
  }

}

/**
 * 基于线程池实现
 *
 * @param sqlFun
 * @param resultFun
 * @param jdbc
 * @tparam T
 * @tparam R
 */

class MySQLASyncFunction[T: TypeInformation, R: TypeInformation](sqlFun: T => String, resultFun: Map[String, _] => R, jdbc: Properties, capacity: Int = 10) extends RichAsyncFunction[T, R] with Logger {

  @transient private[this] var executorService: ExecutorService = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    executorService = Executors.newFixedThreadPool(capacity)
  }

  override def close(): Unit = {
    super.close()
    if (!executorService.isShutdown) {
      executorService.shutdown()
    }
  }

  @throws[Exception]
  def asyncInvoke(input: T, resultFuture: ResultFuture[R]): Unit = {

    CompletableFuture.supplyAsync(new Supplier[Iterable[Map[String, _]]] {
      override def get(): Iterable[Map[String, _]] = JdbcUtils.select(sqlFun(input))(jdbc)
    }, executorService).thenAccept(new Consumer[Iterable[Map[String, _]]] {
      override def accept(result: Iterable[Map[String, _]]): Unit = resultFuture.complete(result.map(resultFun))
    })

  }

  override def timeout(input: T, resultFuture: ResultFuture[R]): Unit = {
    logger.warn("[Streamx] MySQLASync request timeout. retrying... ")
    asyncInvoke(input, resultFuture)
  }
}

class HikariCPDataSourceProvider extends io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider {
  override def getDataSource(json: JsonObject): DataSource = {
    val config = new HikariConfig
    json.filterNot(_.getKey == "provider_class").foreach(entry => {
      val value = entry.getValue.toString
      entry.getKey match {
        case "dataSourceClassName" => config.setDataSourceClassName(value)
        case "jdbcUrl" => config.setJdbcUrl(value)
        case "username" => config.setUsername(value)
        case "password" => config.setPassword(value)
        case "autoCommit" => config.setAutoCommit(value.toBoolean)
        case "connectionTimeout" => config.setConnectionTimeout(value.toLong)
        case "idleTimeout" => config.setIdleTimeout(value.toLong)
        case "maxLifetime" => config.setMaxLifetime(value.toLong)
        case "connectionTestQuery" => config.setConnectionTestQuery(value)
        case "minimumIdle" => config.setMinimumIdle(value.toInt)
        case "maximumPoolSize" => config.setMaximumPoolSize(value.toInt)
        case "poolName" => config.setPoolName(value)
        case "initializationFailTimeout" => config.setInitializationFailTimeout(value.toLong)
        case "isolationInternalQueries" => config.setIsolateInternalQueries(value.toBoolean)
        case "allowPoolSuspension" => config.setAllowPoolSuspension(value.toBoolean)
        case "readOnly" => config.setReadOnly(value.toBoolean)
        case "registerMBeans" => config.setRegisterMbeans(value.toBoolean)
        case "catalog" => config.setCatalog(value)
        case "connectionInitSql" => config.setConnectionInitSql(value)
        case "driverClassName" => config.setDriverClassName(value)
        case "transactionIsolation" => config.setTransactionIsolation(value)
        case "validationTimeout" => config.setValidationTimeout(value.toLong)
        case "leakDetectionThreshold" => config.setLeakDetectionThreshold(value.toLong)
        case "datasource" =>
          for (key <- entry.getValue.asInstanceOf[JsonObject]) {
            config.addDataSourceProperty(key.getKey, key.getValue)
          }
        case "metricRegistry" => throw new UnsupportedOperationException(entry.getKey)
        case "healthCheckRegistry" => throw new UnsupportedOperationException(entry.getKey)
        case "dataSource" => throw new UnsupportedOperationException(entry.getKey)
        case "threadFactory" => throw new UnsupportedOperationException(entry.getKey)
      }
    })
    new HikariDataSource(config)
  }

}
