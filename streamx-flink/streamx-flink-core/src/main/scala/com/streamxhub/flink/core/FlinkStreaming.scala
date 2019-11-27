package com.streamxhub.flink.core

import com.streamxhub.flink.core.conf.Const._
import com.streamxhub.flink.core.util.{Logger, SystemPropertyUtil}
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}

import scala.annotation.meta.getter
import scala.util.Try

trait FlinkStreaming extends Logger {

  @(transient@getter)
  private var env: StreamExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: StreamingContext = _

  def handler(context: StreamingContext): Unit

  private def initialize(args: Array[String]): Unit = {
    SystemPropertyUtil.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])
    //parameter from args...
    val argsMap = ParameterTool.fromArgs(args).toMap
    val file = argsMap.get(FLINK_CONF)
    require(file != null, s"Properties file $file is not found!!!")
    //parameter from properties
    val propMap = ParameterTool.fromPropertiesFile(file).toMap
    //union args and properties...
    val map = new java.util.HashMap[String, String]()
    map.putAll(propMap)
    map.putAll(argsMap)
    parameter = ParameterTool.fromMap(map)

    env = StreamExecutionEnvironment.getExecutionEnvironment
    val checkpointInterval = Try(map.get(KEY_FLINK_CHECKPOINT_INTERVAL).toInt).getOrElse(1000)
    val checkpointMode = Try(CheckpointingMode.valueOf(map.get(KEY_FLINK_CHECKPOINT_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
    val timeCharacteristic = Try(TimeCharacteristic.valueOf(map.get(KEY_FLINK_TIME_CHARACTERISTIC))).getOrElse(TimeCharacteristic.EventTime)
    env.setStreamTimeCharacteristic(timeCharacteristic)
    env.enableCheckpointing(checkpointInterval)
    env.getCheckpointConfig.setCheckpointingMode(checkpointMode)

  }

  /**
   * 用户可覆盖次方法...
   *
   * @param env
   */
  def beforeStart(env: StreamExecutionEnvironment): Unit = {}

  private def createContext(): Unit = {
    context = new StreamingContext(parameter, env)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(env)
    createContext()
    handler(context)
    doStart()
  }

  def doStart(): JobExecutionResult = {
    val appName = parameter.get(APP_NAME, "")
    logger.info(
      s"""
        |   ____   __   _          __          ____  __                            _
        |  / __/  / /  (_)  ___   / /__       / __/ / /_  ____ ___  ___ _  __ _   (_)  ___   ___ _
        | / _/   / /  / /  / _ \\ /  '_/      _\\ \\  / __/ / __// -_)/ _ `/ /  ' \\ / /  / _ \\ / _ `/
        |/_/    /_/  /_/  /_//_//_/\\_\\      /___/  \\__/ /_/   \\__/ \\_,_/ /_/_/_//_/  /_//_/ \\_, /
        |
        |$appName Starting...
        |
        |""".stripMargin)
    env.execute(appName)
  }

}

class StreamingContext(val parameter: ParameterTool, val env: StreamExecutionEnvironment)


