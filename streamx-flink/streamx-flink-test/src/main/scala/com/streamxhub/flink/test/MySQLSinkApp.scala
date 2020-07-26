package com.streamxhub.flink.test

import java.util.Random

import com.streamxhub.flink.core.sink.{JdbcSink, MySQLSink}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._


object MySQLSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    val source = context.addSource(new OrderBeanSource())
    MySQLSink(context).sink[OrderBean](source)(x => {
      if(x.userId == 1000) {
        9/0
      }
      s"insert into orders(id,timestamp) values(${x.userId},${x.timestamp})"
    }).name("jdbcSink")
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

