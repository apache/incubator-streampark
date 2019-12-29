package com.streamxhub.flink.core

import com.streamxhub.flink.core.conf.ConfigConst._
import com.streamxhub.flink.core.util.{Logger, PropertiesUtils, SystemPropertyUtils}
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.api.common.restartstrategy.RestartStrategies

import scala.collection.JavaConversions._
import scala.annotation.meta.getter
import scala.util.Try

trait FlinkStreaming extends Logger {

  @(transient@getter)
  private var env: StreamExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: StreamingContext = _

  def config(env: StreamExecutionEnvironment): Unit = {}

  def handler(context: StreamingContext): Unit

  private def initialize(args: Array[String]): Unit = {
    //read config and merge config......
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[XStreaming])
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.toMap.get(APP_CONF) match {
      case null | "" => KEY_APP_DEFAULT_CONF
      case file => if (file.startsWith("/")) file else s"/${file}"
    }
    val configFile = classOf[XStreaming].getResourceAsStream(config)
    require(configFile != null, s"appConfig file $configFile is not found!!!")

    val configArgs = config.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(configFile)
      case "yml" => PropertiesUtils.fromYamlFile(configFile)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,muse be properties or yml")
    }
    parameter = ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties)

    //init env....
    env = StreamExecutionEnvironment.getExecutionEnvironment

    val parallelism = Try(parameter.get(KEY_FLINK_PARALLELISM).toInt).getOrElse(5)
    val restartAttempts = Try(parameter.get(KEY_FLINK_RESTART_ATTEMPTS).toInt).getOrElse(3)
    val delayBetweenAttempts = Try(parameter.get(KEY_FLINK_DELAY_ATTEMPTS).toInt).getOrElse(50000)
    val timeCharacteristic = Try(TimeCharacteristic.valueOf(parameter.get(KEY_FLINK_TIME_CHARACTERISTIC))).getOrElse(TimeCharacteristic.EventTime)
    env.setParallelism(parallelism)
    env.setStreamTimeCharacteristic(timeCharacteristic)
    env.getConfig.setRestartStrategy(RestartStrategies.fixedDelayRestart(restartAttempts, delayBetweenAttempts))

    //checkPoint
    val checkpointInterval = Try(parameter.get(KEY_FLINK_CHECKPOINT_INTERVAL).toInt).getOrElse(1000)
    val checkpointMode = Try(CheckpointingMode.valueOf(parameter.get(KEY_FLINK_CHECKPOINT_MODE))).getOrElse(CheckpointingMode.EXACTLY_ONCE)
    env.enableCheckpointing(checkpointInterval)
    env.getCheckpointConfig.setCheckpointingMode(checkpointMode)
    //set config by yourself...
    this.config(env)
    env.getConfig.disableSysoutLogging
    env.getConfig.setGlobalJobParameters(parameter)
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
    val appName = parameter.get(KEY_APP_NAME, "")
    val logo =
      s"""
         |███████╗██╗     ██╗███╗   ██╗██╗  ██╗    ███████╗████████╗██████╗ ███████╗ █████╗ ███╗   ███╗██╗███╗   ██╗ ██████╗
         |██╔════╝██║     ██║████╗  ██║██║ ██╔╝    ██╔════╝╚══██╔══╝██╔══██╗██╔════╝██╔══██╗████╗ ████║██║████╗  ██║██╔════╝
         |█████╗  ██║     ██║██╔██╗ ██║█████╔╝     ███████╗   ██║   ██████╔╝█████╗  ███████║██╔████╔██║██║██╔██╗ ██║██║  ███╗
         |██╔══╝  ██║     ██║██║╚██╗██║██╔═██╗     ╚════██║   ██║   ██╔══██╗██╔══╝  ██╔══██║██║╚██╔╝██║██║██║╚██╗██║██║   ██║
         |██║     ███████╗██║██║ ╚████║██║  ██╗    ███████║   ██║   ██║  ██║███████╗██║  ██║██║ ╚═╝ ██║██║██║ ╚████║╚██████╔╝
         |╚═╝     ╚══════╝╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝    ╚══════╝   ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝
         |
         |$appName Starting...
         |
         |""".stripMargin

    println(s"\033[33;4m${logo}\033[0m")

    env.execute(appName)
  }

}

/**
 * 不要觉得神奇,这个类就是这么神奇....
 *
 * @param parameter
 * @param env
 */
class StreamingContext(val parameter: ParameterTool, val streamExecutionEnvironment: StreamExecutionEnvironment) extends StreamExecutionEnvironment(streamExecutionEnvironment.getJavaEnv) {
}


