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

package com.streamxhub.streamx.flink.connector.jdbc.bean

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.vertx.core.json.JsonObject

import javax.sql.DataSource
import scala.collection.JavaConversions._


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
