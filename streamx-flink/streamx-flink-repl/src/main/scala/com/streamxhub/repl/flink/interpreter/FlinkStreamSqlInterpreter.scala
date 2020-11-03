package com.streamxhub.repl.flink.interpreter

import java.io.IOException
import java.util.Properties

import com.streamxhub.repl.flink.interpreter.sql.{AppendStreamSqlJob, SingleRowStreamSqlJob, UpdateStreamSqlJob}
import org.apache.zeppelin.interpreter.{Interpreter, InterpreterContext, InterpreterException}
import org.apache.zeppelin.scheduler.{Scheduler, SchedulerFactory}

import scala.util.Try


class FlinkStreamSqlInterpreter(properties: Properties) extends FlinkSqlInterrpeter(properties) {
  override protected def isBatch = false

  @throws[InterpreterException] override def open(): Unit = {
    this.flinkInterpreter = getInterpreterInTheSameSessionByClassName(classOf[FlinkInterpreter])
    this.tbenv = flinkInterpreter.getJavaStreamTableEnvironment("blink")
    super.open()
  }

  @throws[InterpreterException] override def close(): Unit = {
  }

  @throws[IOException] override def callInnerSelect(sql: String, context: InterpreterContext): Unit = {
    val streamType = Try(context.getLocalProperties.get("type").toLowerCase()).getOrElse(null)
    val streamJob = streamType match {
      case null => throw new IOException("type must be specified for stream sql")
      case "single" =>
        new SingleRowStreamSqlJob(
          flinkInterpreter.getStreamExecutionEnvironment,
          tbenv,
          flinkInterpreter.getJobManager,
          context,
          flinkInterpreter.getDefaultParallelism,
          flinkInterpreter.getFlinkShims
        )
      case "append" =>
        new AppendStreamSqlJob(
          flinkInterpreter.getStreamExecutionEnvironment,
          flinkInterpreter.getStreamTableEnvironment,
          flinkInterpreter.getJobManager,
          context,
          flinkInterpreter.getDefaultParallelism,
          flinkInterpreter.getFlinkShims
        )
      case "update" =>
        new UpdateStreamSqlJob(
          flinkInterpreter.getStreamExecutionEnvironment,
          flinkInterpreter.getStreamTableEnvironment,
          flinkInterpreter.getJobManager,
          context,
          flinkInterpreter.getDefaultParallelism,
          flinkInterpreter.getFlinkShims
        )
      case _ => throw new IOException(s"Unrecognized stream type: $streamType ")
    }
    streamJob.run(sql)
  }

  @throws[IOException] override def callInsertInto(sql: String, context: InterpreterContext): Unit = {
    super.callInsertInto(sql, context)
  }

  @throws[InterpreterException] override def cancel(context: InterpreterContext): Unit = {
    this.flinkInterpreter.cancel(context)
  }

  @throws[InterpreterException] override def getFormType = Interpreter.FormType.SIMPLE

  @throws[InterpreterException] override def getProgress(context: InterpreterContext) = 0

  override def getScheduler: Scheduler = {
    val maxConcurrency = getProperty("zeppelin.flink.concurrentStreamSql.max", "10").toInt
    SchedulerFactory.singleton.createOrGetParallelScheduler(classOf[FlinkStreamSqlInterpreter].getName + this.hashCode, maxConcurrency)
  }
}