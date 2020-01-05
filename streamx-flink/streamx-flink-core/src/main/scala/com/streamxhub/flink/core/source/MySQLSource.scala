package com.streamxhub.flink.core.source

import java.util.Properties

import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.util.{Logger, MySQLUtils}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map


class MySQLSource(@transient val ctx: StreamingContext, specialKafkaParams: Map[String, String] = Map.empty[String, String]) {
  implicit val typeInfo = TypeInformation.of[List[Map[String, _]]](classOf[List[Map[String, _]]])
  def getDataStream(querySQL: String)(implicit config: Properties): DataStream[List[Map[String, _]]] = {
    val mysqlFun = new MySQLSourceFunction(querySQL)
    ctx.addSource(mysqlFun)
  }

}

private[this] class MySQLSourceFunction(querySQL: String)(implicit config: Properties) extends SourceFunction[List[Map[String, _]]] with Logger {
  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  @throws[Exception]
  override def run(ctx: SourceFunction.SourceContext[List[Map[String, _]]]): Unit = {
    while (isRunning) {
      val list = MySQLUtils.select(querySQL)
      ctx.collect(list)
      Thread.sleep(2000)
    }
  }

}