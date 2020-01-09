package com.streamxhub.flink.core.source

import java.util.Properties

import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.util.{Logger, MySQLUtils}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map


class MySQLSource(@transient val ctx: StreamingContext, specialKafkaParams: Map[String, String] = Map.empty[String, String]) {

  def getDataStream[R: TypeInformation](querySQL: String, fun: Map[String, _] => R)(implicit config: Properties): DataStream[R] = {
    val mysqlFun = new MySQLSourceFunction[R](querySQL, fun)
    ctx.addSource(mysqlFun)
  }

}

private[this] class MySQLSourceFunction[R: TypeInformation](querySQL: String, fun: Map[String, _] => R)(implicit config: Properties) extends SourceFunction[R] with Logger {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  @throws[Exception]
  override def run(ctx: SourceFunction.SourceContext[R]): Unit = {
    while (isRunning) {
      val list = MySQLUtils.select(querySQL)
      list.foreach(x => ctx.collect(fun(x)))
    }
  }
}