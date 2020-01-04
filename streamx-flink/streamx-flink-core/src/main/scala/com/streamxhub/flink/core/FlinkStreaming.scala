package com.streamxhub.flink.core

import com.streamxhub.flink.core.conf.ConfigConst._
import com.streamxhub.flink.core.util.{Logger, PropertiesUtils, SystemPropertyUtils}
import org.apache.flink.api.common.{ExecutionConfig, JobExecutionResult}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

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
    val config = argsMap.get(APP_CONF, null) match {
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
    parameter = ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties)

    env = StreamExecutionEnvironment.getExecutionEnvironment
    //init env...
    val parallelism = Try(parameter.get(KEY_FLINK_PARALLELISM).toInt).getOrElse(ExecutionConfig.PARALLELISM_DEFAULT)
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
    env.getConfig.setGlobalJobParameters(parameter)
  }

  /**
   * 用户可覆盖次方法...
   *
   * @param env
   */
  def beforeStart(env: StreamExecutionEnvironment): Unit = {}

  private[this] def createContext(): Unit = {
    context = new StreamingContext(parameter, env)
  }

  private[this] def doStart(): JobExecutionResult = {
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
         |         //======= StreamX let's flink|spark easy...
         |
         |""".stripMargin
    println(s"\033[95;1m${logo}\033[1m\n")
    val appName = parameter.get(KEY_APP_NAME, "")
    println(s"$appName Starting...")
    env.execute(appName)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(env)
    createContext()
    handler(context)
    doStart()
  }

  /**
   *
   * 增强方法.........
   *
   * @param dataStream
   * @tparam T
   * @return
   */

  implicit def sideOut[T: TypeInformation](dataStream: DataStream[T]) = new SiteOutSupport(dataStream)

  class SiteOutSupport[T: TypeInformation](val dataStream: DataStream[T]) {

    def sideOut[R: TypeInformation](sideTag: String, fun: T => R): DataStream[T] = dataStream.process(new ProcessFunction[T, T] {
      val tag = new OutputTag[R](sideTag)

      override def processElement(value: T, ctx: ProcessFunction[T, T]#Context, out: Collector[T]): Unit = {
        val outData = fun(value)
        if (outData != null) {
          ctx.output(tag, outData)
        }
        //侧输出流不能影响主输出流...
        out.collect(value)
      }
    })

    def sideGet[R: TypeInformation](sideTag: String): DataStream[R] = dataStream.getSideOutput(new OutputTag[R](sideTag))

  }

}

/**
 * 不要觉得神奇,这个类就是这么神奇....
 *
 * @param parameter
 * @param environment
 */
class StreamingContext(val parameter: ParameterTool, val environment: StreamExecutionEnvironment) extends StreamExecutionEnvironment(environment.getJavaEnv) {
}




