package com.streamxhub.flink.test

import com.streamxhub.flink.core.sink.JdbcSink
import com.streamxhub.flink.core.source.KafkaSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._

object MySQLSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    //one topic
    val source = new KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })

    JdbcSink(context,parallelism = 3).towPCSink[String](source)(x => {
      s"insert into orders(id,timestamp) values('$x',${System.currentTimeMillis()})"
    }).uid("mysqlSink").name("mysqlSink")


  }

}

/**
 *
 * @param userId    : 用户Id
 * @param timestamp : 下单时间
 */
case class OrderBean(userId: Long,
                     timestamp: Long) extends Serializable

class OrderBeanSource extends SourceFunction[OrderBean] {
  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  var count = 0

  override def run(ctx: SourceFunction.SourceContext[OrderBean]): Unit = {
    while (true) {
      val order = OrderBean(count, System.currentTimeMillis)
      ctx.collect(order)
      count += 1
    }
  }

}

