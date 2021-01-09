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
package com.streamxhub.flink.core.scala

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{Logger, SystemPropertyUtils}
import com.streamxhub.flink.core.scala.ext.DataStreamExt
import com.streamxhub.flink.core.scala.util.{FlinkStreamingInitializer, StreamEnvConfig}
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._


/**
 * @author benjobs
 * @param parameter
 * @param environment
 */
class StreamingContext(val parameter: ParameterTool, private val environment: StreamExecutionEnvironment) extends StreamExecutionEnvironment(environment.getJavaEnv) {

  /**
   * for scala
   *
   * @param args
   */
  def this(args: (ParameterTool, StreamExecutionEnvironment)) = this(args._1, args._2)

  /**
   * for Java
   *
   * @param args
   */
  def this(args: StreamEnvConfig) = this(FlinkStreamingInitializer.initJavaStream(args))

  /**
   * 推荐使用该Api启动任务...
   *
   * @return
   */
  def start(): JobExecutionResult = execute()

  @Deprecated
  override def execute(): JobExecutionResult = {
    val appName = (parameter.get(KEY_APP_NAME(), null), parameter.get(KEY_FLINK_APP_NAME, null)) match {
      case (appName: String, _) => appName
      case (null, appName: String) => appName
      case _ => ""
    }
    execute(appName)
  }

  @Deprecated
  override def execute(jobName: String): JobExecutionResult = {
    println(s"\033[95;1m$LOGO\033[1m\n")
    println(s"[StreamX] FlinkStreaming $jobName Starting...")
    super.execute(jobName)
  }
}


trait FlinkStreaming extends Logger {

  final implicit def streamExt[T: TypeInformation](dataStream: DataStream[T]): DataStreamExt.DataStream[T] = new DataStreamExt.DataStream[T](dataStream)

  final implicit def procFuncExt[IN: TypeInformation, OUT: TypeInformation](ctx: ProcessFunction[IN, OUT]#Context): DataStreamExt.ProcessFunction[IN, OUT] = new DataStreamExt.ProcessFunction[IN, OUT](ctx)

  final implicit lazy val parameter: ParameterTool = context.parameter

  private[this] var context: StreamingContext = _

  var jobExecutionResult: JobExecutionResult = _

  final def main(args: Array[String]): Unit = {
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])
    context = new StreamingContext(FlinkStreamingInitializer.initStream(args, config))
    beforeStart(context)
    handle(context)
    jobExecutionResult = context.start()
  }

  /**
   * 用户可覆盖次方法...
   *
   */
  def beforeStart(context: StreamingContext): Unit = {}

  def config(env: StreamExecutionEnvironment, parameter: ParameterTool): Unit = {}

  def handle(context: StreamingContext): Unit

}



