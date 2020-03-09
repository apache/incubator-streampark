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

  def config(env: StreamExecutionEnvironment, parameter: ParameterTool): Unit = {}

  def handler(context: StreamingContext): Unit

  private def initialize(args: Array[String]): Unit = {
    //read config and merge config......
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_FLINK_APP_CONF, null) match {
      case null | "" => throw new ExceptionInInitializerError("[StreamX] Usage:can't fond config,please set \"--flink.conf $path \" in main arguments")
      case file => file
    }
    val configFile = new java.io.File(config)
    require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
    val configArgs = config.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
      case "yml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
    }

    this.parameter = ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties())

    env = StreamExecutionEnvironment.getExecutionEnvironment
    //init env...
    val parallelism = Try(parameter.get(KEY_FLINK_PARALLELISM).toInt).getOrElse(ExecutionConfig.PARALLELISM_DEFAULT)
    val restartAttempts = Try(parameter.get(KEY_FLINK_RESTART_ATTEMPTS).toInt).getOrElse(3)
    val delayBetweenAttempts = Try(parameter.get(KEY_FLINK_DELAY_ATTEMPTS).toInt).getOrElse(50000)
    val timeCharacteristic = Try(TimeCharacteristic.valueOf(parameter.get(KEY_FLINK_TIME_CHARACTERISTIC))).getOrElse(TimeCharacteristic.EventTime)
    env.setParallelism(parallelism)
    env.setStreamTimeCharacteristic(timeCharacteristic)
    //重启策略.
    env.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(restartAttempts, delayBetweenAttempts))

    //checkPoint,从配置文件读取是否开启checkpoint,默认不启用.
    val enableCheckpoint = Try(parameter.get(KEY_FLINK_CHECKPOINT_ENABLE).toBoolean).getOrElse(false)
    if (enableCheckpoint) {
      val cpInterval = Try(parameter.get(KEY_FLINK_CHECKPOINT_INTERVAL).toInt).getOrElse(1000)
      val cpMode = Try(CheckpointingMode.valueOf(parameter.get(KEY_FLINK_CHECKPOINT_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
      val cpCleanUp = Try(ExternalizedCheckpointCleanup.valueOf(parameter.get(KEY_FLINK_CHECKPOINT_CLEANUP))).getOrElse(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
      val cpTimeout = Try(parameter.get(KEY_FLINK_CHECKPOINT_TIMEOUT).toInt).getOrElse(60000)
      val cpMaxConcurrent = Try(parameter.get(KEY_FLINK_CHECKPOINT_MAX_CONCURRENT).toInt).getOrElse(1)
      val cpMinPauseBetween = Try(parameter.get(KEY_FLINK_CHECKPOINT_MIN_PAUSEBETWEEN).toInt).getOrElse(500)

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

  private[this] def doStart(): JobExecutionResult = {
    println(s"\033[95;1m${LOGO}\033[1m\n")
    val appName = parameter.get(KEY_FLINK_APP_NAME, "")
    println(s"[StreamX] FlinkStreaming $appName Starting...")
    context.execute(appName)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(context)
    handler(context)
    doStart()
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




