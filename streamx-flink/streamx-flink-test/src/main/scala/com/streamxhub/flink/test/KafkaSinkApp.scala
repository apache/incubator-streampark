package com.streamxhub.flink.test

import com.streamxhub.flink.core.scala.sink.KafkaSink
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._

import scala.util.Random

object KafkaSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    val source = new BehaviorSource()
    val ds = context.addSource[Behavior](source).map(_.toString)
    KafkaSink(context).sink(ds)
  }

}


case class Behavior(user_id: String,
                    item_id: Long,
                    category_id: Long,
                    behavior: String,
                    ts: Long) {
  override def toString: String = {
    s"""
       |{
       |user_id:$user_id,
       |item_id:$item_id,
       |category_id:$category_id,
       |behavior:$behavior,
       |ts:$ts
       |}
       |""".stripMargin
  }
}


class BehaviorSource extends SourceFunction[Behavior] {
  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()
  var index = 0

  override def run(ctx: SourceFunction.SourceContext[Behavior]): Unit = {
    val seq = Seq("view", "click", "search", "buy", "share")
    while (isRunning && index <= 10000) {
      index += 1
      val user_id = random.nextInt(1000)
      val item_id = random.nextInt(100)
      val category_id = random.nextInt(20)
      val behavior = seq(random.nextInt(5))
      val order = Behavior(user_id.toString, item_id, category_id, behavior, System.currentTimeMillis())
      ctx.collect(order)
    }
  }

}


