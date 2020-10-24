package com.streamxhub.repl.flink.interpreter

import java.io.IOException
import java.util.Properties

import org.apache.zeppelin.interpreter.Interpreter.FormType
import org.apache.zeppelin.interpreter.{InterpreterContext, InterpreterException}
import org.apache.zeppelin.scheduler.{Scheduler, SchedulerFactory}


class FlinkBatchSqlInterpreter(val prop: Properties) extends FlinkSqlInterrpeter(prop) {
  private var flinkContext: FlinkReplContext = _

  override protected def isBatch = true

  @throws[InterpreterException] override def open(): Unit = {
    this.flinkInterpreter = getInterpreterInTheSameSessionByClassName(classOf[FlinkInterpreter])
    this.tbenv = flinkInterpreter.getJavaBatchTableEnvironment("blink")
    this.flinkContext = flinkInterpreter.getReplContext
    super.open()
  }

  @throws[InterpreterException] override def close(): Unit = {}

  @throws[IOException] override def callInnerSelect(sql: String, context: InterpreterContext): Unit = {
    val table = this.tbenv.sqlQuery(sql)
    flinkContext.setCurrentSql(sql)
    val result = flinkContext.showData(table)
    context.out.write(result)
  }

  @throws[InterpreterException] override def cancel(context: InterpreterContext): Unit = {
    flinkInterpreter.cancel(context)
  }

  @throws[InterpreterException] override def getFormType = FormType.SIMPLE

  @throws[InterpreterException] override def getProgress(context: InterpreterContext): Int = flinkInterpreter.getProgress(context)

  override def getScheduler: Scheduler = {
    val maxConcurrency = properties.getProperty("zeppelin.flink.concurrentBatchSql.max", "10").toInt
    SchedulerFactory.singleton.createOrGetParallelScheduler(classOf[FlinkBatchSqlInterpreter].getName, maxConcurrency)
  }
}
