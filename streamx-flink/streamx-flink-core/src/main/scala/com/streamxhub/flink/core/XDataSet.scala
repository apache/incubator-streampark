package com.streamxhub.flink.core

import com.streamxhub.flink.core.conf.Const._
import com.streamxhub.flink.core.util.Logger
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._

import scala.annotation.meta.getter
import scala.util.Try

trait XDataSet extends Logger {

  @(transient@getter)
  private var env: ExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: DataSetContext = _

  def handler(context: DataSetContext): Unit

  private def initialize(args: Array[String]): Unit = {
    //parameter from args...
    val argsMap = ParameterTool.fromArgs(args).toMap
    val file = Try(argsMap.get(FLINK_CONF)).getOrElse(null)
    require(file != null, s"[StreamX-Flink] Properties file $file is not found!!!")
    //parameter from properties
    val propMap = ParameterTool.fromPropertiesFile(file).toMap
    //union args and properties...
    val map = new java.util.HashMap[String, String]()
    map.putAll(propMap)
    map.putAll(argsMap)
    parameter = ParameterTool.fromMap(map)

    env = ExecutionEnvironment.getExecutionEnvironment
  }

  /**
   * 用户可覆盖次方法...
   *
   * @param env
   */
  def beforeStart(env: ExecutionEnvironment): Unit = {}

  private def createContext(): Unit = {
    context = new DataSetContext(parameter, env)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(env)
    createContext()
    doStart()
    handler(context)
  }

  def doStart(): Unit = {
    val appName = parameter.get(APP_NAME, "")
    logger.info(
      s"""
         |
         |   ____   __   _          __          ___         __          ____       __
         |  / __/  / /  (_)  ___   / /__       / _ \\ ___ _ / /_ ___ _  / __/ ___  / /_
         | / _/   / /  / /  / _ \\ /  '_/      / // // _ `// __// _ `/ _\\ \\  / -_)/ __/
         |/_/    /_/  /_/  /_//_//_/\\_\\      /____/ \\_,_/ \\__/ \\_,_/ /___/  \\__/ \\__/
         |
         |
         |$appName Starting....
         |
         |""".stripMargin)
  }

}

class DataSetContext(val parameter: ParameterTool, val env: ExecutionEnvironment)


