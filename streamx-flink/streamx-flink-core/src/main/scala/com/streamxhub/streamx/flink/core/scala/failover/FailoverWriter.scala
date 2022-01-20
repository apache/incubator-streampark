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

package com.streamxhub.streamx.flink.core.scala.failover

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util._
import com.streamxhub.streamx.flink.core.scala.failover.FailoverStorageType.{FailoverStorageType, HBase, HDFS, Kafka, MySQL}
import org.apache.hadoop.conf.{Configuration => HConf}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.hbase.client.{BufferedMutator, BufferedMutatorParams, Put, RetriesExhaustedWithDetailsException, Connection => HBaseConn}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HColumnDescriptor, HConstants, HTableDescriptor, TableName}
import org.apache.hadoop.io.IOUtils
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}

import java.io.ByteArrayInputStream
import java.net.URI
import java.util._
import java.util.concurrent.locks.ReentrantLock
import scala.collection.JavaConversions._


class FailoverWriter(failoverStorage: FailoverStorageType, properties: Properties) extends AutoCloseable with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new ReentrantLock()
  }

  private var kafkaProducer: KafkaProducer[String, String] = _
  private var hConnect: HBaseConn = _
  private var mutator: BufferedMutator = _
  private var fileSystem: FileSystem = _


  def write(request: SinkRequest): Unit = {
    this.synchronized {
      val table = request.table.split("\\.").last
      failoverStorage match {
        case Kafka =>
          if (!Lock.initialized) {
            try {
              Lock.lock.lock()
              if (!Lock.initialized) {
                Lock.initialized = true
                properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                kafkaProducer = new KafkaProducer[String, String](properties)
              }
            } catch {
              case exception: Exception => {
                logError(s"build Failover storageType:KAFKA failed exception ${exception.getStackTrace}")
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
          kafkaProducer.send(record, new Callback() {
            override def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
              logInfo(s"Failover successful!! storageType:Kafka,table: $table,size:${request.size}")
            }
          }).get()

        case MySQL =>
          if (!Lock.initialized) {
            try {
              Lock.lock.lock()
              if (!Lock.initialized) {
                Lock.initialized = true
                properties.put(KEY_ALIAS, s"failover-${table}")
                val mysqlConnect = JdbcUtils.getConnection(properties)
                val mysqlTable = mysqlConnect.getMetaData.getTables(null, null, table, Array("TABLE", "VIEW"))
                if (!mysqlTable.next()) {
                  JdbcUtils.execute(
                    mysqlConnect,
                    s"create table $table (`values` text, `timestamp` bigint)"
                  )
                  logWarn(s"Failover storageType:MySQL,table: $table is not exist,auto created...")
                }
              }
            } catch {
              case exception: Exception => {
                logError(s"build Failover storageType:MySQL failed exception ${exception.getStackTrace}")
                throw exception
              }
            } finally {
              Lock.lock.unlock()
            }
          }
          val timestamp = System.currentTimeMillis()
          val records = request.records.map(x => {
            val v = cleanUp(x)
            s""" ($v,$timestamp) """.stripMargin
          })
          val sql = s"INSERT INTO $table(`values`,`timestamp`) VALUES ${records.mkString(",")} "
          JdbcUtils.update(sql)(properties)
          logInfo(s"Failover successful!! storageType:MySQL,table: $table,size:${request.size}")

        case HBase =>
          val tableName = TableName.valueOf(table)
          val familyName = "cf"
          if (!Lock.initialized) {
            try {
              Lock.lock.lock()
              if (!Lock.initialized) {
                Lock.initialized = true
                hConnect = HBaseClient(properties).connection
                val admin = hConnect.getAdmin
                if (!admin.tableExists(tableName)) {
                  val desc = new HTableDescriptor(tableName)
                  desc.addFamily(new HColumnDescriptor(familyName))
                  admin.createTable(desc)
                  logInfo(s"Failover storageType:HBase,table: $table is not exist,auto created...")
                }
                val mutatorParam = new BufferedMutatorParams(tableName)
                  .listener(new BufferedMutator.ExceptionListener {
                    override def onException(exception: RetriesExhaustedWithDetailsException, mutator: BufferedMutator): Unit = {
                      for (i <- 0.until(exception.getNumExceptions)) {
                        logInfo(s"Failover storageType:HBase Failed to sent put ${exception.getRow(i)},error:${exception.getLocalizedMessage}")
                      }
                    }
                  })
                mutator = hConnect.getBufferedMutator(mutatorParam)
              }
            } catch {
              case exception: Exception => {
                logError(s"build Failover storageType:HBase failed exception ${exception.getStackTrace}")
                throw exception
              }
            } finally {
              Lock.lock.unlock()
            }
          }
          val timestamp = System.currentTimeMillis()
          for (i <- 0 until request.size) {
            val rowKey = HConstants.LATEST_TIMESTAMP - timestamp - i //you know?...
            val put = new Put(Bytes.toBytes(rowKey))
              .addColumn(familyName.getBytes, "values".getBytes, Bytes.toBytes(request.records(i)))
              .addColumn(familyName.getBytes, "timestamp".getBytes, Bytes.toBytes(timestamp))
            mutator.mutate(put)
          }
          mutator.flush()

        case HDFS =>
          val path = properties("path")
          val format = properties.getOrElse("format", DateUtils.format_yyyyMMdd)
          require(path != null)
          val fileName = s"$path/$table"
          val rootPath = new Path(s"$fileName/${DateUtils.format(new Date(), format)}")
          try {
            if (!Lock.initialized) {
              try {
                Lock.lock.lock()
                if (!Lock.initialized) {
                  Lock.initialized = true
                  fileSystem = (Option(properties("namenode")), Option(properties("user"))) match {
                    case (None, None) => FileSystem.get(new HConf())
                    case (Some(nn), Some(u)) => FileSystem.get(new URI(nn), new HConf(), u)
                    case (Some(nn), _) => FileSystem.get(new URI(nn), new HConf())
                    case _ => throw new IllegalArgumentException("[StreamX] usage error..")
                  }
                }
              } catch {
                case exception: Exception => {
                  logError(s"build Failover storageType:HDFS failed exception ${exception.getStackTrace}")
                  throw exception
                }
              } finally {
                Lock.lock.unlock()
              }
            }
            val uuid = UUID.randomUUID().toString.replace("-", "")
            val filePath = new Path(s"$rootPath/${System.currentTimeMillis()}_$uuid")
            var outStream = fileSystem.create(filePath)
            var record = new StringBuilder
            request.records.foreach(x => record.append(x).append("\n"))
            var inputStream = new ByteArrayInputStream(record.toString().getBytes)
            IOUtils.copyBytes(inputStream, outStream, 1024, true)
            record.clear()
            record = null
            inputStream = null
            outStream = null
          } catch {
            case e: Exception => e.printStackTrace()
          }
        case _ => throw new UnsupportedOperationException(s"[StreamX] unsupported failover storageType:$failoverStorage")
      }
    }
  }

  private[this] def cleanUp(record: String) = {
    s""" "${record.replace("\"", "\\\"")}" """.stripMargin
  }

  override def close(): Unit = {
    if (kafkaProducer != null) kafkaProducer.close()
    if (fileSystem != null) fileSystem.close()
    if (mutator != null) {
      mutator.flush()
      mutator.close()
    }
  }

}

