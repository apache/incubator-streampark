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
    env.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(restartAttempts, delayBetweenAttempts))

    //checkPoint,从配置文件读取是否开启checkpoint,默认开启
    val enableCheckpoint = Try(parameter.get(KEY_FLINK_CHECKPOINT_ENABLE).toBoolean).getOrElse(true)
    if(enableCheckpoint) {
      val checkpointInterval = Try(parameter.get(KEY_FLINK_CHECKPOINT_INTERVAL).toInt).getOrElse(1000)
      val checkpointMode = Try(CheckpointingMode.valueOf(parameter.get(KEY_FLINK_CHECKPOINT_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
      env.enableCheckpointing(checkpointInterval)
      env.getCheckpointConfig.setCheckpointingMode(checkpointMode)
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




