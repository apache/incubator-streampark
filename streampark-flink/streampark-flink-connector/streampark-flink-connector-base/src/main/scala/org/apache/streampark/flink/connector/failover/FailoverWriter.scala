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

package org.apache.streampark.flink.connector.failover

import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.util._
import org.apache.streampark.flink.connector.conf.FailoverStorageType._

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}

import java.util._
import java.util.concurrent.locks.ReentrantLock

import org.apache.streampark.common.util.Implicits._

class FailoverWriter(failoverStorage: FailoverStorageType, properties: Properties)
  extends AutoCloseable
  with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new ReentrantLock()
  }

  private var kafkaProducer: KafkaProducer[String, String] = _

  def write(request: SinkRequest): Unit = {
    this.synchronized {
      val table = request.table.split("\\.").last
      failoverStorage match {
        case NONE =>
        case Console =>
          val records = request.records.map(x => s"(${cleanUp(x)})")
          logInfo(s"failover body: [ ${records.mkString(",")} ]")
        case Kafka =>
          if (!Lock.initialized) {
            try {
              Lock.lock.lock()
              if (!Lock.initialized) {
                Lock.initialized = true
                properties.put(
                  "key.serializer",
                  "org.apache.kafka.common.serialization.StringSerializer")
                properties.put(
                  "value.serializer",
                  "org.apache.kafka.common.serialization.StringSerializer")
                kafkaProducer = new KafkaProducer[String, String](properties)
              }
            } catch {
              case exception: Exception => {
                logError(
                  s"build Failover storageType:KAFKA failed exception ${exception.getStackTrace
                      .mkString("Array(", ", ", ")")}")
                throw exception
              }
            } finally {
              Lock.lock.unlock()
            }
          }
          val topic = properties.getProperty(KEY_KAFKA_TOPIC)
          val timestamp = System.currentTimeMillis()
          val records = request.records.map(cleanUp)
          val sendData =
            s"""
               |{
               |"values":[${records.mkString(",")}],
               |"timestamp":$timestamp
               |}
               |""".stripMargin
          val record = new ProducerRecord[String, String](topic, sendData)
          kafkaProducer
            .send(
              record,
              new Callback() {
                override def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
                  logInfo(
                    s"Failover successful!! storageType:Kafka,table: $table,size:${request.size}")
                }
              }
            )
            .get()
        case MySQL =>
          if (!Lock.initialized) {
            try {
              Lock.lock.lock()
              if (!Lock.initialized) {
                Lock.initialized = true
                properties.put(KEY_ALIAS, s"failover-$table")
                val mysqlConnect = JdbcUtils.getConnection(properties)
                val mysqlTable =
                  mysqlConnect.getMetaData.getTables(null, null, table, Array("TABLE", "VIEW"))
                if (!mysqlTable.next()) {
                  JdbcUtils.execute(
                    mysqlConnect,
                    s"create table $table (`values` text, `timestamp` bigint)")
                  logWarn(s"Failover storageType:MySQL,table: $table is not exist,auto created...")
                }
              }
            } catch {
              case exception: Exception =>
                logError(
                  s"build Failover storageType:MySQL failed exception ${exception.getStackTrace}")
                throw exception
            } finally {
              Lock.lock.unlock()
            }
          }
          val timestamp = System.currentTimeMillis()
          val records = request.records.map(
            x => {
              val v = cleanUp(x)
              s""" ($v,$timestamp) """.stripMargin
            })
          val sql = s"INSERT INTO $table(`values`,`timestamp`) VALUES ${records.mkString(",")} "
          JdbcUtils.update(sql)(properties)
          logInfo(s"Failover successful!! storageType:MySQL,table: $table,size:${request.size}")
        case _ =>
          throw new UnsupportedOperationException(
            s"[StreamPark] unsupported failover storageType:$failoverStorage")
      }
    }
  }

  private[this] def cleanUp(record: String) = {
    s""" "${record.replace("\"", "\\\"")}" """.stripMargin
  }

  override def close(): Unit = {
    if (kafkaProducer != null) kafkaProducer.close()
  }

}
