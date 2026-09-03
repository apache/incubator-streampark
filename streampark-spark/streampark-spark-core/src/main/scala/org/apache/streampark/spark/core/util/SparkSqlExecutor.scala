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

package org.apache.streampark.spark.core.util

import org.apache.streampark.common.configuration.{CommandLineParser, ConfigurationParser}
import org.apache.streampark.common.configuration.option.ApplicationOptions
import org.apache.streampark.common.util.{DeflaterUtils, Logger}
import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.spark.configuration.SparkRuntimeOptions

import org.apache.commons.lang3.StringUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object SparkSqlExecutor extends Logger {

  private val lock = new ReentrantReadWriteLock().writeLock

  def runBatch(args: Array[String]): Unit = {
    val sparkSession = createSparkSession(args)
    try {
      executeSql(args, sparkSession)
    } finally {
      sparkSession.sparkContext.stop()
    }
  }

  def runStreaming(args: Array[String]): Unit = {
    val sparkSession = createSparkSession(args)
    val sparkConf = sparkSession.sparkContext.getConf
    val checkpoint = resolveCheckpoint(args)
    val createOnError = resolveCreateOnError(args)

    val context = checkpoint match {
      case "" =>
        new StreamingContext(
          sparkSession.sparkContext,
          Seconds(sparkConf.get(SparkRuntimeOptions.BATCH_DURATION_SECONDS.key).toInt))
      case checkpointPath =>
        def createContext(): StreamingContext =
          new StreamingContext(
            sparkSession.sparkContext,
            Seconds(sparkConf.get(SparkRuntimeOptions.BATCH_DURATION_SECONDS.key).toInt))
        val tmpContext =
          StreamingContext.getOrCreate(checkpointPath, createContext _, createOnError = createOnError)
        tmpContext.checkpoint(checkpointPath)
        tmpContext
    }

    try {
      executeSql(args, sparkSession)
      context.start()
      context.awaitTermination()
    } finally {
      context.stop(stopSparkContext = false, stopGracefully = true)
    }
  }

  private def createSparkSession(args: Array[String]): SparkSession = {
    val sparkConf = initSparkConf(args)
    val builder = SparkSession.builder().config(sparkConf)
    if (sparkConf.getBoolean("spark.config.enable.hive.support", defaultValue = false)) {
      builder.enableHiveSupport()
    }
    builder.getOrCreate()
  }

  private def executeSql(args: Array[String], sparkSession: SparkSession): Unit = {
    val sparkSql = {
      val sql = CommandLineParser.parse(args).get(ApplicationOptions.SQL)
      require(StringUtils.isNotBlank(sql), "Usage: spark sql cannot be null")
      Try(DeflaterUtils.unzipString(sql)) match {
        case Success(value) => value
        case Failure(_) =>
          throw new IllegalArgumentException("Usage: spark sql is invalid or null, please check")
      }
    }

    SqlCommandParser.parseSQL(sparkSql).foreach { command =>
      val operands = if (command.operands.isEmpty) null else command.operands.head
      try {
        lock.lock()
        sparkSession.sql(command.originSql)
        logInfo(s"${command.command.name}:$operands")
      } finally {
        if (lock.isHeldByCurrentThread) {
          lock.unlock()
        }
      }
    }
  }

  private def initSparkConf(args: Array[String]): SparkConf = {
    val sparkConf = new SparkConf()
    var argv = args.toList
    var conf: String = null
    val userArgs = ArrayBuffer[(String, String)]()

    while (argv.nonEmpty) {
      argv match {
        case "--conf" :: value :: tail =>
          conf = value
          argv = tail
        case "--checkpoint" :: _ :: tail =>
          argv = tail
        case "--createOnError" :: _ :: tail =>
          argv = tail
        case Nil =>
        case other :: value :: tail if other.startsWith(CommandLineParser.LONG_OPTION_PREFIX) =>
          userArgs += other.drop(2) -> value
          argv = tail
        case tail =>
          logError(s"Unrecognized options: ${tail.mkString(" ")}")
          printUsageAndExit()
      }
    }

    if (conf != null) {
      val localConf = ConfigurationParser.parse(java.nio.file.Paths.get(conf)).toMap.asScala
      localConf.foreach(arg => sparkConf.set(arg._1, arg._2))
    }
    userArgs.foreach(arg => sparkConf.set(arg._1, arg._2))

    val appMain = sparkConf.get(SparkRuntimeOptions.MAIN_CLASS.key, "org.apache.streampark.spark.cli.SqlClient")
    if (appMain == null) {
      logError(s"[StreamPark] parameter: ${SparkRuntimeOptions.MAIN_CLASS.key} must not be empty!")
      System.exit(1)
    }

    val appName = sparkConf.get(SparkRuntimeOptions.APPLICATION_NAME.key, null) match {
      case null | "" => appMain
      case name => name
    }

    if (sparkConf.get("spark.master", null) == "local") {
      sparkConf.setAppName(s"[LocalDebug] $appName").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }
    sparkConf.set("spark.streaming.stopGracefullyOnShutdown", "true")
    sparkConf
  }

  private def resolveCheckpoint(args: Array[String]): String = {
    var argv = args.toList
    var checkpoint = ""
    while (argv.nonEmpty) {
      argv match {
        case "--checkpoint" :: value :: tail =>
          checkpoint = value
          argv = tail
        case _ :: _ :: tail =>
          argv = tail
        case _ :: Nil =>
          argv = Nil
        case Nil =>
      }
    }
    checkpoint
  }

  private def resolveCreateOnError(args: Array[String]): Boolean = {
    var argv = args.toList
    var createOnError = true
    while (argv.nonEmpty) {
      argv match {
        case "--createOnError" :: value :: tail =>
          createOnError = value.toBoolean
          argv = tail
        case _ :: _ :: tail =>
          argv = tail
        case _ :: Nil =>
          argv = Nil
        case Nil =>
      }
    }
    createOnError
  }

  private def printUsageAndExit(): Unit = {
    logError(
      """
        |"Usage: Streaming [options]
        |
        | Options are:
        |   --checkpoint <checkpoint dir>
        |   --createOnError <Failed to recover from checkpoint, whether to recreated, true or false>
        |""".stripMargin)
    System.exit(1)
  }

}
