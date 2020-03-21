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

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{Logger, PropertiesUtils, SystemPropertyUtils}
import org.apache.flink.api.common.{ExecutionConfig, JobExecutionResult}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.contrib.streaming.state.{DefaultConfigurableOptionsFactory, RocksDBStateBackend}
import com.streamxhub.flink.core.enums.{StateBackend => XStateBackend}
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.runtime.state.memory.MemoryStateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector


import scala.collection.JavaConversions._
import scala.util.Try

/**
 * 不要觉得神奇,这个类就是这么神奇....
 *
 * @author benjobs
 * @param parameter
 * @param environment
 */
class StreamingContext(val parameter: ParameterTool, val environment: StreamExecutionEnvironment) extends StreamExecutionEnvironment(environment.getJavaEnv) {
  val paramMap: java.util.Map[String, String] = parameter.toMap
}

trait FlinkStreaming extends Logger {

  implicit def ext[T: TypeInformation](dataStream: DataStream[T]): DataStreamExt[T] = new DataStreamExt(dataStream)

  @transient
  private var env: StreamExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: StreamingContext = _

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(context)
    handler(context)
    doStart()
  }

  /**
   *
   * @param args
   */
  private def initialize(args: Array[String]): Unit = {

    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])

    //read config and merge config......
    this.parameter = initParameter(args)

    env = StreamExecutionEnvironment.getExecutionEnvironment

    initEnvConfig()

    //checkpoint
    checkpoint()

    //set config by yourself...
    this.config(env, parameter)

    env.getConfig.setGlobalJobParameters(parameter)

    context = new StreamingContext(parameter, env)
  }

  /**
   * 用户可覆盖次方法...
   *
   */
  def beforeStart(context: StreamingContext): Unit = {}

  def config(env: StreamExecutionEnvironment, parameter: ParameterTool): Unit = {}

  def handler(context: StreamingContext): Unit

  private[this] def doStart(): JobExecutionResult = {
    println(s"\033[95;1m$LOGO\033[1m\n")
    val appName = parameter.get(KEY_FLINK_APP_NAME, "")
    println(s"[StreamX] FlinkStreaming $appName Starting...")
    context.execute(appName)
  }

  private[this] def initParameter(args: Array[String]): ParameterTool = {
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_FLINK_APP_CONF, null) match {
      case null | "" => throw new ExceptionInInitializerError("[StreamX] Usage:can't fond config,please set \"--flink.conf $path \" in main arguments")
      case file => file
    }
    val configFile = new java.io.File(config)
    require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
    val configArgs = config.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
      case "yml" | "yaml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
    }
    ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties())
  }

  private[this] def initEnvConfig(): Unit = {
    //init env...
    val parallelism = Try(parameter.get(KEY_FLINK_PARALLELISM).toInt).getOrElse(ExecutionConfig.PARALLELISM_DEFAULT)
    val restartAttempts = Try(parameter.get(KEY_FLINK_RESTART_ATTEMPTS).toInt).getOrElse(3)
    val delayBetweenAttempts = Try(parameter.get(KEY_FLINK_DELAY_ATTEMPTS).toInt).getOrElse(50000)
    val timeCharacteristic = Try(TimeCharacteristic.valueOf(parameter.get(KEY_FLINK_TIME_CHARACTERISTIC))).getOrElse(TimeCharacteristic.EventTime)
    env.setParallelism(parallelism)
    env.setStreamTimeCharacteristic(timeCharacteristic)
    //重启策略.
    env.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(restartAttempts, delayBetweenAttempts))
  }

  private[this] def checkpoint(): Unit = {
    //checkPoint,从配置文件读取是否开启checkpoint,默认不启用.
    val enableCheckpoint = Try(parameter.get(KEY_FLINK_CHECKPOINTS_ENABLE).toBoolean).getOrElse(false)
    if (enableCheckpoint) {
      val cpInterval = Try(parameter.get(KEY_FLINK_CHECKPOINTS_INTERVAL).toInt).getOrElse(1000)
      val cpMode = Try(CheckpointingMode.valueOf(parameter.get(KEY_FLINK_CHECKPOINTS_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
      val cpCleanUp = Try(ExternalizedCheckpointCleanup.valueOf(parameter.get(KEY_FLINK_CHECKPOINTS_CLEANUP))).getOrElse(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
      val cpTimeout = Try(parameter.get(KEY_FLINK_CHECKPOINTS_TIMEOUT).toInt).getOrElse(60000)
      val cpMaxConcurrent = Try(parameter.get(KEY_FLINK_CHECKPOINTS_MAX_CONCURRENT).toInt).getOrElse(1)
      val cpMinPauseBetween = Try(parameter.get(KEY_FLINK_CHECKPOINTS_MIN_PAUSEBETWEEN).toInt).getOrElse(500)

      //默认:开启检查点,1s进行启动一个检查点
      env.enableCheckpointing(cpInterval)
      //默认:模式为exactly_one，精准一次语义
      env.getCheckpointConfig.setCheckpointingMode(cpMode)
      //默认: 检查点之间的时间间隔为1s【checkpoint最小间隔】
      env.getCheckpointConfig.setMinPauseBetweenCheckpoints(cpMinPauseBetween)
      //默认:检查点必须在1分钟之内完成，或者被丢弃【checkpoint超时时间】
      env.getCheckpointConfig.setCheckpointTimeout(cpTimeout)
      //默认:同一时间只允许进行一次检查点
      env.getCheckpointConfig.setMaxConcurrentCheckpoints(cpMaxConcurrent)
      //默认:被cancel会保留Checkpoint数据
      env.getCheckpointConfig.enableExternalizedCheckpoints(cpCleanUp)
    }

    val stateBackend = Try(XStateBackend.withName(parameter.get(KEY_FLINK_STATE_BACKEND))).getOrElse(null)
    if (stateBackend != null) {
      val dataDir = if (stateBackend == XStateBackend.jobmanager) null else {
        /**
         * dataDir如果从配置文件中读取失败(key:flink.checkpoints.dir),则尝试从flink-conf.yml中读取..
         */
        Try(parameter.get(KEY_FLINK_CHECKPOINTS_DIR)).getOrElse(null) match {
          //从flink-conf.yaml中读取.
          case null =>
            logWarn("[StreamX] can't found flink.checkpoints.dir from properties,now try found from flink-conf.yaml")
            val flinkHome = System.getenv("FLINK_HOME")
            require(flinkHome != null, "[StreamX] FLINK_HOME is not defined in your system.")
            val flinkConf = s"$flinkHome/conf/flink-conf.yaml"
            val prop = PropertiesUtils.fromYamlFile(flinkConf)
            //从flink-conf.yaml中读取,key: state.checkpoints.dir
            val dir = prop("state.checkpoints.dir")
            require(dir != null, s"[StreamX] can't found flink.checkpoints.dir from $flinkConf ")
            logInfo(s"[StreamX] stat.backend: flink.checkpoints.dir found in flink-conf.yaml,$dir")
            dir
          case dir => {
            logInfo(s"[StreamX] stat.backend: flink.checkpoints.dir found in properties,$dir")
            dir
          }
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
          env.setStateBackend(ms)
        case XStateBackend.filesystem =>
          logInfo(s"[StreamX] stat.backend Type: filesystem...")
          val async = Try(parameter.get(KEY_FLINK_STATE_BACKEND_ASYNC).toBoolean).getOrElse(false)
          val fs = new FsStateBackend(dataDir, async)
          env.setStateBackend(fs)
        case XStateBackend.rocksdb =>
          logInfo(s"[StreamX] stat.backend Type: rocksdb...")
          // 默认开启增量.
          val incremental = Try(parameter.get(KEY_FLINK_STATE_BACKEND_INCREMENTAL).toBoolean).getOrElse(true)
          val rs = new RocksDBStateBackend(dataDir, incremental)
          /**
           * @see <a href="https://ci.apache.org/projects/flink/flink-docs-release-1.9/ops/config.html#rocksdb-configurable-options"/>Flink Rocksdb Config</a>
           */
          val map = new java.util.HashMap[String, Object]()
          val sinkKey = List(KEY_FLINK_STATE_BACKEND_ASYNC, KEY_FLINK_STATE_BACKEND_INCREMENTAL, KEY_FLINK_STATE_BACKEND_MEMORY, KEY_FLINK_STATE_ROCKSDB)
          parameter.getProperties.filter(_._1.startsWith(KEY_FLINK_STATE_ROCKSDB)).filter(x => sinkKey.exists(k => k != x)).foreach(x => map.put(x._1, x._2))
          if (map.nonEmpty) {
            val optionsFactory = new DefaultConfigurableOptionsFactory
            val config = new Configuration()
            val confData = classOf[Configuration].getDeclaredField("confData")
            confData.setAccessible(true)
            confData.set(map, config)
            optionsFactory.configure(config)
            rs.setOptions(optionsFactory)
          }
          env.setStateBackend(rs)
        case _ =>
          logError("[StreamX] usage error!!! stat.backend must be (jobmanager|filesystem|rocksdb)")
      }
    }

  }

}

class DataStreamExt[T: TypeInformation](val dataStream: DataStream[T]) {

  def sideOut[R: TypeInformation](sideTag: String, fun: T => R, skip: Boolean = false): DataStream[T] = dataStream.process(new ProcessFunction[T, T] {
    val tag = new OutputTag[R](sideTag)

    override def processElement(value: T, ctx: ProcessFunction[T, T]#Context, out: Collector[T]): Unit = {
      val outData = fun(value)
      ctx.output(tag, outData)
      //根据条件判断是否跳过主输出...
      if (!skip) {
        out.collect(value)
      }
    }
  })

  def sideGet[R: TypeInformation](sideTag: String): DataStream[R] = dataStream.getSideOutput(new OutputTag[R](sideTag))

}

