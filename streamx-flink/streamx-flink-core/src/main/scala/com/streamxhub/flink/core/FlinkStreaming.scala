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
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])
    val argsMap = ParameterTool.fromArgs(args)
    /**
     * 线上环境会自动加上启动参数app.debug=false,会走线上一套外部参数的方式....
     * debug本地调试需要设置-Dapp.conf=$configPath... 指定配置文件地址
     */
    val configArgs = SystemPropertyUtils.getBoolean(APP_DEBUG,false) match {
      case true =>
        //debug...
        val config = SystemPropertyUtils.get(APP_CONF,null) match {
          case null | "" => throw new ExceptionInInitializerError("can't fond config,please set \"-Dapp.conf=$path \" in VM Option")
          case file => file
        }
        val configFile = new java.io.File(config)
        require(configFile.exists(), s"appConfig file $configFile is not found!!!")
        config.split("\\.").last match {
          case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
          case "yml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,muse be properties or yml")
        }
      case _ => Map.empty[String,String]
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
         |
         |                         ▒▓██▓██▒
         |                     ▓████▒▒█▓▒▓███▓▒
         |                  ▓███▓░░        ▒▒▒▓██▒  ▒
         |                ░██▒   ▒▒▓▓█▓▓▒░      ▒████
         |                ██▒         ░▒▓███▒    ▒█▒█▒
         |                  ░▓█            ███   ▓░▒██
         |                    ▓█       ▒▒▒▒▒▓██▓░▒░▓▓█
         |                  █░ █   ▒▒░       ███▓▓█ ▒█▒▒▒
         |                  ████░   ▒▓█▓      ██▒▒▒ ▓███▒
         |               ░▒█▓▓██       ▓█▒    ▓█▒▓██▓ ░█░
         |         ▓░▒▓████▒ ██         ▒█    █▓░▒█▒░▒█▒
         |        ███▓░██▓  ▓█           █   █▓ ▒▓█▓▓█▒
         |      ░██▓  ░█░            █  █▒ ▒█████▓▒ ██▓░▒
         |     ███░ ░ █░          ▓ ░█ █████▒░░    ░█░▓  ▓░
         |    ██▓█ ▒▒▓▒          ▓███████▓░       ▒█▒ ▒▓ ▓██▓
         | ▒██▓ ▓█ █▓█       ░▒█████▓▓▒░         ██▒▒  █ ▒  ▓█▒
         | ▓█▓  ▓█ ██▓ ░▓▓▓▓▓▓▓▒              ▒██▓           ░█▒
         | ▓█    █ ▓███▓▒░              ░▓▓▓███▓          ░▒░ ▓█
         | ██▓    ██▒    ░▒▓▓███▓▓▓▓▓██████▓▒            ▓███  █
         |▓███▒ ███   ░▓▓▒░░   ░▓████▓░                  ░▒▓▒  █▓
         |█▓▒▒▓▓██  ░▒▒░░░▒▒▒▒▓██▓░                            █▓
         |██ ▓░▒█   ▓▓▓▓▒░░  ▒█▓       ▒▓▓██▓    ▓▒          ▒▒▓
         |▓█▓ ▓▒█  █▓░  ░▒▓▓██▒            ░▓█▒   ▒▒▒░▒▒▓█████▒
         | ██░ ▓█▒█▒  ▒▓▓▒  ▓█                █░      ░░░░   ░█▒
         | ▓█   ▒█▓   ░     █░                ▒█              █▓
         |  █▓   ██         █░                 ▓▓        ▒█▓▓▓▒█░
         |   █▓ ░▓██░       ▓▒                  ▓█▓▒░░░▒▓█░    ▒█
         |    ██   ▓█▓░      ▒                    ░▒█▒██▒      ▓▓
         |     ▓█▒   ▒█▓▒░                         ▒▒ █▒█▓▒▒░░▒██
         |      ░██▒    ▒▓▓▒                     ▓██▓▒█▒ ░▓▓▓▓▒█▓
         |        ░▓██▒                          ▓░  ▒█▓█  ░░▒▒▒
         |            ▒▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒░░▓▓  ▓░▒█░
         |
         |$appName Starting...
         |
         |""".stripMargin

    println(s"${logo}")

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


