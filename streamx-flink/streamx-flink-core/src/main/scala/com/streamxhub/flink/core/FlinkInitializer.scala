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
package com.streamxhub.flink.core

import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{Logger, PropertiesUtils}
import com.streamxhub.flink.core.enums.ApiType.ApiType
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.contrib.streaming.state.{DefaultConfigurableOptionsFactory, RocksDBStateBackend}
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.runtime.state.memory.MemoryStateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import com.streamxhub.flink.core.enums.{ApiType, StateBackend => XStateBackend}
import com.streamxhub.flink.core.function.StreamEnvConfigFunction
import org.apache.flink.api.scala.ExecutionEnvironment
import com.streamxhub.flink.core.enums.RestartStrategy
import org.apache.commons.codec.digest.DigestUtils
import org.apache.flink.api.common.time.Time
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

import scala.collection.JavaConversions._
import scala.util.Try

object FlinkInitializer {

  private var flinkInitializer: FlinkInitializer = _

  def get(args: Array[String], config: (StreamExecutionEnvironment, ParameterTool) => Unit = null): FlinkInitializer = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkInitializer(args, config)
        }
      }
    }
    flinkInitializer
  }

  def get(args: StreamEnvConfig): FlinkInitializer = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkInitializer(args)
        }
      }
    }
    flinkInitializer
  }

}


class StreamEnvConfig(val args: Array[String], val conf: StreamEnvConfigFunction) {

}

class FlinkInitializer private(args: Array[String], apiType: ApiType) extends Logger {

  def this(args: Array[String], func: (StreamExecutionEnvironment, ParameterTool) => Unit) = {
    this(args, ApiType.Scala)
    streamConfFunc = func
    initStreamEnv()
  }

  def this(streamEnvConfig: StreamEnvConfig) = {
    this(streamEnvConfig.args, ApiType.JAVA)
    javaEnvConf = streamEnvConfig.conf
    initStreamEnv()
  }

  private[this] var streamConfFunc: (StreamExecutionEnvironment, ParameterTool) => Unit = _

  private[this] var javaEnvConf: StreamEnvConfigFunction = _

  private[this] var datasetConfFun: (ExecutionEnvironment, ParameterTool) => Unit = _

  val parameter: ParameterTool = initParameter()

  private[this] var streamEnv: StreamExecutionEnvironment = _

  def readFlinkConf(config: String): Map[String, String] = {
    val extension = config.split("\\.").last.toLowerCase

    def decode(x: String): String = {
      val text = x.drop(7)
      val byte = Base64.getDecoder decode text
      new String(byte, "UTF-8")
    }

    config match {
      case x: String if x.startsWith("prop://") =>
        PropertiesUtils.fromPropertiesText(decode(x))
      case x: String if x.startsWith("yaml://") =>
        PropertiesUtils.fromYamlText(decode(x))
      case _ =>
        val configFile = new File(config)
        require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
          case "yml" | "yaml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
        }
    }
  }

  private[this] def initParameter(): ParameterTool = {
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_FLINK_APP_CONF(), null) match {
      case null | "" => throw new ExceptionInInitializerError("[StreamX] Usage:can't fond config,please set \"--flink.conf $path \" in main arguments")
      case file => file
    }
    val configArgs = readFlinkConf(config)
    ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties())
  }

  def streamEnvironment: StreamExecutionEnvironment = {
    if (streamEnv == null) {
      this.synchronized {
        if (streamEnv == null) {
          initStreamEnv()
        }
      }
    }
    streamEnv
  }


  private[this] def initStreamEnv() = {
    this.streamEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //init env...
    val parallelism = Try(parameter.get(KEY_FLINK_PARALLELISM).toInt).getOrElse(ExecutionConfig.PARALLELISM_DEFAULT)
    val timeCharacteristic = Try(TimeCharacteristic.valueOf(parameter.get(KEY_FLINK_WATERMARK_TIME_CHARACTERISTIC))).getOrElse(TimeCharacteristic.EventTime)
    val interval = Try(parameter.get(KEY_FLINK_WATERMARK_INTERVAL).toInt).getOrElse(0)
    if (interval > 0) {
      streamEnv.getConfig.setAutoWatermarkInterval(interval)
    }
    streamEnv.setParallelism(parallelism)
    streamEnv.setStreamTimeCharacteristic(timeCharacteristic)

    //重启策略.
    restartStrategy()

    //checkpoint
    checkpoint()

    apiType match {
      case ApiType.JAVA if javaEnvConf != null => javaEnvConf.envConfig(this.streamEnv.getJavaEnv, this.parameter)
      case ApiType.Scala if streamConfFunc != null => streamConfFunc(this.streamEnv, this.parameter)
      case _ =>
    }

    this.streamEnv.getConfig.setGlobalJobParameters(parameter)

    this.streamEnv
  }

  private[this] def restartStrategy(): Unit = {

    def getTimeUnit(time: String, default: (Int, TimeUnit) = (5, TimeUnit.SECONDS)): (Int, TimeUnit) = {
      val timeUnit = time match {
        case "" => null
        case x: String =>
          val num = x.replaceAll("\\s+|[a-z|A-Z]+$", "").toInt
          val unit = x.replaceAll("^[0-9]+|\\s+", "") match {
            case "" => null
            case "s" => TimeUnit.SECONDS
            case "m" | "min" => TimeUnit.MINUTES
            case "h" => TimeUnit.HOURS
            case "d" | "day" => TimeUnit.DAYS
            case _ => throw new IllegalArgumentException()
          }
          (num, unit)
      }
      timeUnit match {
        case null => default
        case other if other._2 == null => (other._1 / 1000, TimeUnit.SECONDS) //未带单位,值必须为毫秒,这里转成对应的秒...
        case other => other
      }
    }

    val strategy = Try(RestartStrategy.byName(parameter.get(KEY_FLINK_RESTART_STRATEGY))).getOrElse(null)
    strategy match {
      case RestartStrategy.`failure-rate` =>

        /**
         * restart-strategy.failure-rate.max-failures-per-interval: 在一个Job认定为失败之前,最大的重启次数
         * restart-strategy.failure-rate.failure-rate-interval: 计算失败率的时间间隔
         * restart-strategy.failure-rate.delay: 两次连续重启尝试之间的时间间隔
         * e.g:
         * >>>
         * max-failures-per-interval: 10
         * failure-rate-interval: 5 min
         * delay: 2 s
         * <<<
         * 即:每次异常重启的时间间隔是"2秒",如果在"5分钟"内,失败总次数到达"10次" 则任务失败.
         */
        val interval = Try(parameter.get(KEY_FLINK_RESTART_FAILURE_PER_INTERVAL).toInt).getOrElse(3)

        val rateInterval = getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_FAILURE_RATE_INTERVAL)).getOrElse(null), (5, TimeUnit.MINUTES))

        val delay = getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_FAILURE_RATE_DELAY)).getOrElse(null))

        streamEnv.getConfig.setRestartStrategy(RestartStrategies.failureRateRestart(
          interval,
          Time.of(rateInterval._1, rateInterval._2),
          Time.of(delay._1, delay._2)
        ))
      case RestartStrategy.`fixed-delay` =>

        /**
         *
         * restart-strategy.fixed-delay.attempts: 在Job最终宣告失败之前，Flink尝试执行的次数
         * restart-strategy.fixed-delay.delay: 一个任务失败之后不会立即重启,这里指定间隔多长时间重启
         * e.g:
         * attempts: 5,delay: 3 s
         * 即:
         * 任务最大的失败重试次数是5次,每次任务重启的时间间隔是3秒,如果失败次数到达5次,则任务失败退出
         */
        val attempts = Try(parameter.get(KEY_FLINK_RESTART_ATTEMPTS).toInt).getOrElse(3)
        val delay = getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_DELAY)).getOrElse(null))

        /**
         * 任务执行失败后总共重启 restartAttempts 次,每次重启间隔 delayBetweenAttempts
         */
        streamEnv.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(attempts, Time.of(delay._1, delay._2)))

      case RestartStrategy.none => streamEnv.getConfig.setRestartStrategy(RestartStrategies.noRestart())

      case null => logger.info("[StreamX] RestartStrategy not set,use default from $flink_conf")
    }
  }

  private[this] def checkpoint(): Unit = {
    //checkPoint,从配置文件读取是否开启checkpoint,默认不启用.
    val enableCheckpoint = Try(parameter.get(KEY_FLINK_CHECKPOINTS_ENABLE).toBoolean).getOrElse(false)
    if (!enableCheckpoint) return

    val cpInterval = Try(parameter.get(KEY_FLINK_CHECKPOINTS_INTERVAL).toInt).getOrElse(1000)
    val cpMode = Try(CheckpointingMode.valueOf(parameter.get(KEY_FLINK_CHECKPOINTS_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
    val cpCleanUp = Try(ExternalizedCheckpointCleanup.valueOf(parameter.get(KEY_FLINK_CHECKPOINTS_CLEANUP))).getOrElse(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    val cpTimeout = Try(parameter.get(KEY_FLINK_CHECKPOINTS_TIMEOUT).toLong).getOrElse(CheckpointConfig.DEFAULT_TIMEOUT)
    val cpMaxConcurrent = Try(parameter.get(KEY_FLINK_CHECKPOINTS_MAX_CONCURRENT).toInt).getOrElse(CheckpointConfig.DEFAULT_MAX_CONCURRENT_CHECKPOINTS)
    val cpMinPauseBetween = Try(parameter.get(KEY_FLINK_CHECKPOINTS_MIN_PAUSEBETWEEN).toLong).getOrElse(CheckpointConfig.DEFAULT_MIN_PAUSE_BETWEEN_CHECKPOINTS)

    //默认:开启检查点,1s进行启动一个检查点
    streamEnv.enableCheckpointing(cpInterval)
    //默认:模式为exactly_one，精准一次语义
    streamEnv.getCheckpointConfig.setCheckpointingMode(cpMode)
    //默认: 检查点之间的时间间隔为0s【checkpoint最小间隔】
    streamEnv.getCheckpointConfig.setMinPauseBetweenCheckpoints(cpMinPauseBetween)
    //默认:检查点必须在10分钟之内完成，或者被丢弃【checkpoint超时时间】
    streamEnv.getCheckpointConfig.setCheckpointTimeout(cpTimeout)
    //默认:同一时间只允许进行一次检查点
    streamEnv.getCheckpointConfig.setMaxConcurrentCheckpoints(cpMaxConcurrent)
    //默认:被cancel会保留Checkpoint数据
    streamEnv.getCheckpointConfig.enableExternalizedCheckpoints(cpCleanUp)

    val stateBackend = XStateBackend.withName(parameter.get(KEY_FLINK_STATE_BACKEND, null))
    //stateBackend
    if (stateBackend != null) {
      val cpDir = if (stateBackend == XStateBackend.jobmanager) null else {
        /**
         * cpDir如果从配置文件中读取失败(key:state.checkpoints.dir),则尝试从flink-conf.yml中读取..
         */
        parameter.get(KEY_FLINK_STATE_CHECKPOINTS_DIR, null) match {
          //从flink-conf.yaml中读取.
          case null =>
            logWarn("[StreamX] can't found flink.checkpoints.dir from properties,now try found from flink-conf.yaml")
            val flinkConf = {
              //从启动参数中读取配置文件...
              val flinkHome = parameter.get(KEY_FLINK_HOME(), null) match {
                case null | "" =>
                  logInfo("[StreamX] --flink.home is undefined,now try found from flink-conf.yaml on System env.")
                  val flinkHome = System.getenv("FLINK_HOME")
                  require(flinkHome != null, "[StreamX] FLINK_HOME is not defined in your system.")
                  flinkHome
                case file => file
              }
              val flinkConf = s"$flinkHome/conf/flink-conf.yaml"
              readFlinkConf(flinkConf)
            }
            //从flink-conf.yaml中读取,key: state.checkpoints.dir
            val dir = flinkConf(KEY_FLINK_STATE_CHECKPOINTS_DIR)
            require(dir != null, s"[StreamX] can't found flink.checkpoints.dir from $flinkConf ")
            logInfo(s"[StreamX] stat.backend: flink.checkpoints.dir found in flink-conf.yaml,$dir")
            dir
          case dir =>
            logInfo(s"[StreamX] stat.backend: flink.checkpoints.dir found in properties,$dir")
            dir
        }
      }

      stateBackend match {
        /**
         * The size of each individual state is by default limited to 5 MB. This value can be increased in the constructor of the MemoryStateBackend.
         * Irrespective of the configured maximal state size, the state cannot be larger than the akka frame size (see <a href="https://ci.apache.org/projects/flink/flink-docs-release-1.9/ops/config.html">Configuration</a>).
         * The aggregate state must fit into the JobManager memory.
         */
        case XStateBackend.jobmanager =>
          logInfo(s"[StreamX] stat.backend Type: jobmanager...")
          //default 5 MB,cannot be larger than the akka frame size
          val maxMemorySize = Try(parameter.get(KEY_FLINK_STATE_BACKEND_MEMORY).toInt).getOrElse(MemoryStateBackend.DEFAULT_MAX_STATE_SIZE)
          val async = Try(parameter.get(KEY_FLINK_STATE_BACKEND_ASYNC).toBoolean).getOrElse(false)
          val ms = new MemoryStateBackend(maxMemorySize, async)
          streamEnv.setStateBackend(ms)
        case XStateBackend.filesystem =>
          logInfo(s"[StreamX] stat.backend Type: filesystem...")
          val async = Try(parameter.get(KEY_FLINK_STATE_BACKEND_ASYNC).toBoolean).getOrElse(false)
          val fs = new FsStateBackend(cpDir, async)
          streamEnv.setStateBackend(fs)
        case XStateBackend.rocksdb =>
          logInfo(s"[StreamX] stat.backend Type: rocksdb...")
          // 默认开启增量.
          val incremental = Try(parameter.get(KEY_FLINK_STATE_BACKEND_INCREMENTAL).toBoolean).getOrElse(true)
          val rs = new RocksDBStateBackend(cpDir, incremental)
          /**
           * @see <a href="https://ci.apache.org/projects/flink/flink-docs-release-1.9/ops/config.html#rocksdb-configurable-options"/>Flink Rocksdb Config</a>
           */
          val map = new java.util.HashMap[String, Object]()
          val skipKey = List(KEY_FLINK_STATE_BACKEND_ASYNC, KEY_FLINK_STATE_BACKEND_INCREMENTAL, KEY_FLINK_STATE_BACKEND_MEMORY, KEY_FLINK_STATE_ROCKSDB)
          parameter.getProperties.filter(_._1.startsWith(KEY_FLINK_STATE_ROCKSDB)).filterNot(x => skipKey.contains(x._1)).foreach(x => map.put(x._1, x._2))
          if (map.nonEmpty) {
            val optionsFactory = new DefaultConfigurableOptionsFactory
            val config = new Configuration()
            val confData = classOf[Configuration].getDeclaredField("confData")
            confData.setAccessible(true)
            confData.set(map, config)
            optionsFactory.configure(config)
            rs.setRocksDBOptions(optionsFactory)
          }
          streamEnv.setStateBackend(rs)
        case _ =>
          logError("[StreamX] usage error!!! stat.backend must be (jobmanager|filesystem|rocksdb)")
      }
    }
  }

  def initDatasetEnv() = {

  }

}
