package com.streamxhub.repl.flink.interpreter

import java.util.Properties

import com.streamxhub.common.util.ClassLoaderUtils
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

  private var interpreter: FlinkScalaInterpreter = _
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
    this.interpreter = new FlinkScalaInterpreter(getProperties)
    this.interpreter.open()
    this.replContext = this.interpreter.getReplContext
  }

  @throws[InterpreterException] override def close(): Unit = if (this.interpreter != null) this.interpreter.close()

  @throws[InterpreterException] override def interpret(st: String, context: InterpreterContext): InterpreterResult = {
    LOGGER.debug("Interpret code: " + st)
    this.replContext.setInterpreterContext(context)
    this.replContext.setGui(context.getGui)
    this.replContext.setNoteGui(context.getNoteGui)
    // set ClassLoader of current Thread to be the ClassLoader of Flink scala-shell,
    // otherwise codegen will fail to find classes defined in scala-shell
    ClassLoaderUtils.runAsClassLoader(getFlinkScalaShellLoader, () => {
      createPlannerAgain()
      setParallelismIfNecessary(context)
      setSavepointIfNecessary(context)
      interpreter.interpret(st, context)
    })
  }

  @throws[InterpreterException] override def cancel(context: InterpreterContext): Unit = {
    this.interpreter.cancel(context)
  }

  @throws[InterpreterException] override def getFormType = FormType.SIMPLE

  @throws[InterpreterException] override def getProgress(context: InterpreterContext): Int = this.interpreter.getProgress(context)

  @throws[InterpreterException] override def completion(buf: String, cursor: Int, interpreterContext: InterpreterContext): java.util.List[InterpreterCompletion] = interpreter.completion(buf, cursor, interpreterContext)

  private[flink] def getExecutionEnvironment = this.interpreter.getExecutionEnvironment()

  private[flink] def getStreamExecutionEnvironment = this.interpreter.getStreamExecutionEnvironment()

  private[flink] def getStreamTableEnvironment = this.interpreter.getStreamTableEnvironment("blink")

  private[flink] def getJavaBatchTableEnvironment(planner: String) = this.interpreter.getJavaBatchTableEnvironment(planner)

  private[flink] def getJavaStreamTableEnvironment(planner: String) = this.interpreter.getJavaStreamTableEnvironment(planner)

  private[flink] def getBatchTableEnvironment = this.interpreter.getBatchTableEnvironment("blink")

  private[flink] def getJobManager = this.interpreter.getJobManager

  private[flink] def getDefaultParallelism = this.interpreter.getDefaultParallelism

  private[flink] def getDefaultSqlParallelism = this.interpreter.getDefaultSqlParallelism

  /**
   * Workaround for issue of FLINK-16936.
   */
  def createPlannerAgain(): Unit = {
    this.interpreter.createPlannerAgain()
  }

  def getFlinkScalaShellLoader: ClassLoader = interpreter.getFlinkScalaShellLoader

  private[flink] def getReplContext = this.replContext

  private[flink] def getFlinkConfiguration = this.interpreter.getConfiguration

  def getInnerIntp: FlinkScalaInterpreter = this.interpreter

  def getFlinkShims: FlinkShims = this.interpreter.getFlinkShims

  def setSavepointIfNecessary(context: InterpreterContext): Unit = {
    this.interpreter.setSavepointPathIfNecessary(context)
  }

  def setParallelismIfNecessary(context: InterpreterContext): Unit = {
    this.interpreter.setParallelismIfNecessary(context)
  }

}
