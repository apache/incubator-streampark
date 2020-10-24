package com.streamxhub.repl.flink.interpreter

import java.util.Properties

import com.streamxhub.repl.flink.shims.FlinkShims
import org.apache.zeppelin.interpreter.Interpreter.FormType
import org.apache.zeppelin.interpreter.{Interpreter, InterpreterContext, InterpreterException, InterpreterResult}
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion
import org.slf4j.LoggerFactory


/**
 * Interpreter for flink scala. It delegates all the function to FlinkScalaInterpreter.
 */
class FlinkInterpreter(properties: Properties) extends Interpreter(properties) {
  private val LOGGER = LoggerFactory.getLogger(classOf[FlinkInterpreter])

  private var innerIntp: FlinkScalaInterpreter = _
  private var replContext: FlinkReplContext = _

  @throws[InterpreterException] private def checkScalaVersion(): Unit = {
    val scalaVersionString = scala.util.Properties.versionString
    LOGGER.info("Using Scala: " + scalaVersionString)

    /*if (!scalaVersionString.contains("version 2")) {
      throw new InterpreterException("Unsupported scala version: " + scalaVersionString + ", Only scala 2.11 is supported")
    }*/
  }

  @throws[InterpreterException] override def open(): Unit = {
    checkScalaVersion()
    this.innerIntp = new FlinkScalaInterpreter(getProperties)
    this.innerIntp.open()
    this.replContext = this.innerIntp.getReplContext
  }

  @throws[InterpreterException] override def close(): Unit = {
    if (this.innerIntp != null) this.innerIntp.close()
  }

  @throws[InterpreterException] override def interpret(st: String, context: InterpreterContext): InterpreterResult = {
    LOGGER.debug("Interpret code: " + st)
    this.replContext.setInterpreterContext(context)
    this.replContext.setGui(context.getGui)
    this.replContext.setNoteGui(context.getNoteGui)
    // set ClassLoader of current Thread to be the ClassLoader of Flink scala-shell,
    // otherwise codegen will fail to find classes defined in scala-shell
    val originClassLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(getFlinkScalaShellLoader)
      createPlannerAgain()
      setParallelismIfNecessary(context)
      setSavepointIfNecessary(context)
      innerIntp.interpret(st, context)
    } finally Thread.currentThread.setContextClassLoader(originClassLoader)
  }

  @throws[InterpreterException] override def cancel(context: InterpreterContext): Unit = {
    this.innerIntp.cancel(context)
  }

  @throws[InterpreterException] override def getFormType = FormType.SIMPLE

  @throws[InterpreterException] override def getProgress(context: InterpreterContext): Int = this.innerIntp.getProgress(context)

  @throws[InterpreterException] override def completion(buf: String, cursor: Int, interpreterContext: InterpreterContext): java.util.List[InterpreterCompletion] = innerIntp.completion(buf, cursor, interpreterContext)

  private[flink] def getExecutionEnvironment = this.innerIntp.getExecutionEnvironment()

  private[flink] def getStreamExecutionEnvironment = this.innerIntp.getStreamExecutionEnvironment()

  private[flink] def getStreamTableEnvironment = this.innerIntp.getStreamTableEnvironment("blink")

  private[flink] def getJavaBatchTableEnvironment(planner: String) = this.innerIntp.getJavaBatchTableEnvironment(planner)

  private[flink] def getJavaStreamTableEnvironment(planner: String) = this.innerIntp.getJavaStreamTableEnvironment(planner)

  private[flink] def getBatchTableEnvironment = this.innerIntp.getBatchTableEnvironment("blink")

  private[flink] def getJobManager = this.innerIntp.getJobManager

  private[flink] def getDefaultParallelism = this.innerIntp.getDefaultParallelism

  private[flink] def getDefaultSqlParallelism = this.innerIntp.getDefaultSqlParallelism

  /**
   * Workaround for issue of FLINK-16936.
   */
  def createPlannerAgain(): Unit = {
    this.innerIntp.createPlannerAgain()
  }

  def getFlinkScalaShellLoader: ClassLoader = innerIntp.getFlinkScalaShellLoader

  private[flink] def getZeppelinContext = this.replContext

  private[flink] def getFlinkConfiguration = this.innerIntp.getConfiguration

  def getInnerIntp: FlinkScalaInterpreter = this.innerIntp

  def getFlinkShims: FlinkShims = this.innerIntp.getFlinkShims

  def setSavepointIfNecessary(context: InterpreterContext): Unit = {
    this.innerIntp.setSavepointPathIfNecessary(context)
  }

  def setParallelismIfNecessary(context: InterpreterContext): Unit = {
    this.innerIntp.setParallelismIfNecessary(context)
  }

}
