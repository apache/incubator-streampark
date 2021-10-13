/*
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
package com.streamxhub.streamx.flink.core

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.enums.{ApiType, CheckpointStorage, RestartStrategy, StateBackend => XStateBackend}
import com.streamxhub.streamx.common.util._
import org.apache.flink.api.common.RuntimeExecutionMode
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.CoreOptions
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend
import org.apache.flink.runtime.state.storage.{FileSystemCheckpointStorage, JobManagerCheckpointStorage}
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.TableConfig

import java.io.File
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.collection.Map
import scala.util.{Failure, Success, Try}

private[flink] object FlinkStreamingInitializer {

  private[this] var flinkInitializer: FlinkStreamingInitializer = _

  def initStream(args: Array[String], config: (StreamExecutionEnvironment, ParameterTool) => Unit = null): (ParameterTool, StreamExecutionEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkStreamingInitializer(args, ApiType.scala)
          flinkInitializer.streamEnvConfFunc = config
          flinkInitializer.initStreamEnv()
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment)
  }

  def initJavaStream(args: StreamEnvConfig): (ParameterTool, StreamExecutionEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkStreamingInitializer(args.args, ApiType.java)
          flinkInitializer.javaStreamEnvConfFunc = args.conf
          flinkInitializer.initStreamEnv()
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment)
  }
}


private[flink] class FlinkStreamingInitializer(args: Array[String], apiType: ApiType) extends Logger {

  var streamEnvConfFunc: (StreamExecutionEnvironment, ParameterTool) => Unit = _

  var tableConfFunc: (TableConfig, ParameterTool) => Unit = _

  var javaStreamEnvConfFunc: StreamEnvConfigFunction = _

  var javaTableEnvConfFunc: TableEnvConfigFunction = _

  lazy val parameter: ParameterTool = initParameter()

  private[this] var localStreamEnv: StreamExecutionEnvironment = _

  private[this] lazy val defaultFlinkConf: Map[String, String] = {
    parameter.get(KEY_FLINK_CONF(), null) match {
      case null =>
        //通过脚本启动..
        val flinkHome = System.getenv("FLINK_HOME")
        require(flinkHome != null)
        logInfo(s"flinkHome: $flinkHome")
        val yaml = new File(s"$flinkHome/conf/flink-conf.yaml")
        PropertiesUtils.loadFlinkConfYaml(yaml)
      case flinkConf =>
        //从StreamXConsole后端传递过来的.
        PropertiesUtils.loadFlinkConfYaml(DeflaterUtils.unzipString(flinkConf))
    }
  }

  def readFlinkConf(config: String): Map[String, String] = {
    val extension = config.split("\\.").last.toLowerCase

    val map = config match {
      case x if x.startsWith("yaml://") =>
        PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("prop://") =>
        PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("hdfs://") =>

        /**
         * 如果配置文件为hdfs方式,则需要用户将hdfs相关配置文件copy到resources下...
         */
        val text = HdfsUtils.read(x)
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesText(text)
          case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,must be properties or yml")
        }
      case _ =>
        val configFile = new File(config)
        require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
          case "yml" | "yaml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,must be properties or yml")
        }
    }

    map
      .filter(!_._1.startsWith(KEY_FLINK_DEPLOYMENT_OPTION_PREFIX))
      .map(x => x._1.replace(KEY_FLINK_DEPLOYMENT_PROPERTY_PREFIX, "") -> x._2)
  }

  def initParameter(): ParameterTool = {
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_APP_CONF(), null) match {
      case null | "" => throw new ExceptionInInitializerError("[StreamX] Usage:can't fond config,please set \"--conf $path \" in main arguments")
      case file => file
    }
    val configArgs = readFlinkConf(config)
    //显示指定的优先级 > 项目配置文件 > 系统配置文件...
    ParameterTool.fromSystemProperties().mergeWith(ParameterTool.fromMap(configArgs)).mergeWith(argsMap)
  }

  def streamEnvironment: StreamExecutionEnvironment = {
    if (localStreamEnv == null) {
      this.synchronized {
        if (localStreamEnv == null) {
          initStreamEnv()
        }
      }
    }
    localStreamEnv
  }

  def initStreamEnv(): Unit = {
    localStreamEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //init env...
    Try(parameter.get(KEY_FLINK_PARALLELISM()).toInt).getOrElse {
      Try(parameter.get(CoreOptions.DEFAULT_PARALLELISM.key()).toInt).getOrElse(CoreOptions.DEFAULT_PARALLELISM.defaultValue().toInt)
    } match {
      case p if p > 0 => localStreamEnv.setParallelism(p)
      case _ => throw new IllegalArgumentException("[StreamX] parallelism must be > 0. ")
    }
    val interval = Try(parameter.get(KEY_FLINK_WATERMARK_INTERVAL).toInt).getOrElse(0)
    if (interval > 0) {
      localStreamEnv.getConfig.setAutoWatermarkInterval(interval)
    }

    val executionMode = Try(RuntimeExecutionMode.valueOf(parameter.get(KEY_EXECUTION_RUNTIME_MODE))).getOrElse(RuntimeExecutionMode.STREAMING)
    localStreamEnv.setRuntimeMode(executionMode)

    //重启策略.
    restartStrategy()

    //checkpoint
    checkpoint()

    apiType match {
      case ApiType.java if javaStreamEnvConfFunc != null => javaStreamEnvConfFunc.configuration(localStreamEnv.getJavaEnv, parameter)
      case ApiType.scala if streamEnvConfFunc != null => streamEnvConfFunc(localStreamEnv, parameter)
      case _ =>
    }
    localStreamEnv.getConfig.setGlobalJobParameters(parameter)
  }

  private[this] def restartStrategy(): Unit = {
    /**
     * 优先到当前项目的配置下找配置,找不到,则取$FLINK_HOME/conf/flink-conf.yml里的配置
     */
    val prefixLen = "flink.".length
    val strategy = Try(RestartStrategy.byName(parameter.get(KEY_FLINK_RESTART_STRATEGY)))
      .getOrElse(
        Try(RestartStrategy.byName(defaultFlinkConf("restart-strategy"))).getOrElse(null)
      )

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
        val interval = Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_PER_INTERVAL).toInt)
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_PER_INTERVAL.drop(prefixLen)).toInt).getOrElse(3)
          )

        val rateInterval = DateUtils.getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_RATE_INTERVAL))
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_RATE_INTERVAL.drop(prefixLen))).getOrElse(null)
          ), (5, TimeUnit.MINUTES))

        val delay = DateUtils.getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_DELAY))
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_DELAY.drop(prefixLen))).getOrElse(null)
          ))

        streamEnvironment.getConfig.setRestartStrategy(RestartStrategies.failureRateRestart(
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
        val attempts = Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS).toInt)
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS.drop(prefixLen)).toInt).getOrElse(3)
          )

        val delay = DateUtils.getTimeUnit(Try(parameter.get(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_DELAY))
          .getOrElse(
            Try(defaultFlinkConf(KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_DELAY.drop(prefixLen))).getOrElse(null)
          ))

        /**
         * 任务执行失败后总共重启 restartAttempts 次,每次重启间隔 delayBetweenAttempts
         */
        streamEnvironment.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(attempts, Time.of(delay._1, delay._2)))

      case RestartStrategy.none => streamEnvironment.getConfig.setRestartStrategy(RestartStrategies.noRestart())

      case null => logInfo("RestartStrategy not set,use default from $flink_conf")
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
    val unaligned = Try(parameter.get(KEY_FLINK_CHECKPOINTS_UNALIGNED).toBoolean).getOrElse(false)

    //默认:开启检查点,1s进行启动一个检查点
    streamEnvironment.enableCheckpointing(cpInterval)

    val cpConfig = streamEnvironment.getCheckpointConfig

    cpConfig.setCheckpointingMode(cpMode)
    //默认: 检查点之间的时间间隔【checkpoint最小间隔】
    cpConfig.setMinPauseBetweenCheckpoints(cpMinPauseBetween)
    //默认:检查点必须在 $cpTimeout 分钟之内完成，或者被丢弃【checkpoint超时时间】
    cpConfig.setCheckpointTimeout(cpTimeout)
    //默认:同一时间允许进行?次检查点[默认一次]
    cpConfig.setMaxConcurrentCheckpoints(cpMaxConcurrent)
    //默认:被cancel会保留Checkpoint数据
    cpConfig.enableExternalizedCheckpoints(cpCleanUp)
    //非对齐checkpoint (flink 1.11.1 =+)
    cpConfig.enableUnalignedCheckpoints(unaligned)

    val stateBackend = XStateBackend.withName(parameter.get(KEY_FLINK_STATE_BACKEND, null))

    //stateBackend
    if (stateBackend != null) {
      require(
        stateBackend == XStateBackend.hashmap || stateBackend == XStateBackend.rocksdb,
        "state.backend must be [hashmap|rocksdb] in flink 1.13"
      )
      val storage = {
        val storage = parameter.get(KEY_FLINK_STATE_CHECKPOINT_STORAGE, null) match {
          //从flink-conf.yaml中读取.
          case null =>
            logWarn("can't found flink.state.checkpoint-storage from properties,now try found from flink-conf.yaml")
            val storage = defaultFlinkConf("state.checkpoint-storage")
            require(storage != null, s"[StreamX] can't found state.checkpoint-storage from default FlinkConf ")
            logInfo(s"state.checkpoint-storage: state.checkpoint-storage found in flink-conf.yaml,$storage")
            storage
          case storage =>
            logInfo(s"state.checkpoint-storage: flink.checkpoints.dir found in properties,$storage")
            storage
        }

        Try(CheckpointStorage.withName(storage)) match {
          case Success(value) => value
          case Failure(e) => throw new IllegalArgumentException(e)
        }
      }

      lazy val cpDir = parameter.get(KEY_FLINK_STATE_CHECKPOINTS_DIR, null) match {
        //从flink-conf.yaml中读取.
        case null =>
          logWarn("can't found flink.state.checkpoints.dir from properties,now try found from flink-conf.yaml")
          //从flink-conf.yaml中读取,key: state.checkpoints.dir
          val dir = defaultFlinkConf("state.checkpoints.dir")
          require(dir != null, s"[StreamX] can't found state.checkpoints.dir from Default FlinkConf ")
          logInfo(s"stat.backend: state.checkpoints.dir found in flink-conf.yaml,$dir")
          dir
        case dir =>
          logInfo(s"stat.backend: flink.checkpoints.dir found in properties,$dir")
          dir
      }

      stateBackend match {
        case XStateBackend.hashmap =>
          logInfo("stat.backend: hashmap...")
          streamEnvironment.setStateBackend(new HashMapStateBackend())
          storage match {
            case CheckpointStorage.jobmanager =>
              logInfo("state.checkpoint-storage: jobmanager...")
              cpConfig.setCheckpointStorage(new JobManagerCheckpointStorage())
            case CheckpointStorage.filesystem =>
              logInfo("state.checkpoint-storage: filesystem...")
              cpConfig.setCheckpointStorage(new FileSystemCheckpointStorage(cpDir))
          }
        case XStateBackend.rocksdb =>
          logInfo("stat.backend: rocksdb...")
          streamEnvironment.setStateBackend(new EmbeddedRocksDBStateBackend())
          storage match {
            case CheckpointStorage.filesystem =>
              logInfo("state.checkpoint-storage: filesystem...")
              cpConfig.setCheckpointStorage(new FileSystemCheckpointStorage(cpDir))
            case _ =>
              throw new IllegalArgumentException("[StreamX] state.backend is  rocksdb, state.checkpoint-storage must be filesystem...")
          }
      }
    }

  }


}
