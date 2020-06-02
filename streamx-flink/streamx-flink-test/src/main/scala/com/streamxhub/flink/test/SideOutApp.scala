package com.streamxhub.flink.test

import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._

import scala.util.Random

/**
 * 侧输出流
 */
object SideOutApp extends FlinkStreaming {


  override def handler(context: StreamingContext): Unit = {
    val source = context.addSource(new SideSource())
    /**
     * 侧输出流。。。
     * 官方写法:设置侧输出流
     */
    val side1 = source.process(new ProcessFunction[SideEntry, SideEntry] {
      val tag = new OutputTag[SideEntry]("flink")

      override def processElement(value: SideEntry, ctx: ProcessFunction[SideEntry, SideEntry]#Context, out: Collector[SideEntry]): Unit = {
        if (value.userId < 100) {
          ctx.output(tag, value)
        } else {
          out.collect(value)
        }
      }
    })

    //官方写法,获取侧输出流
    side1.getSideOutput(new OutputTag[SideEntry]("flink")).print("flink:========>")

    /**
     * Streamx 封装之后的写法....
     */
    //侧输出写出....
    val side2 = source.sideOut("streamx", _.userId < 100)
    //侧输出获取....
    side2.sideGet("streamx").print("streamx:========>")

  }

}

/**
 *
 * @param userId      : 用户Id
 * @param orderId     : 订单ID
 * @param siteId      : 站点ID
 * @param cityId      : 城市Id
 * @param orderStatus : 订单状态(1:下单,0:退单)
 * @param isNewOrder  : 是否是首单
 * @param price       : 单价
 * @param quantity    : 订单数量
 * @param timestamp   : 下单时间
 */
case class SideEntry(userId: Long,
                     orderId: Long,
                     siteId: Long,
                     cityId: Long,
                     orderStatus: Int,
                     isNewOrder: Int,
                     price: Double,
                     quantity: Int,
                     timestamp: Long)

class SideSource extends SourceFunction[SideEntry] {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()

  override def run(ctx: SourceFunction.SourceContext[SideEntry]): Unit = {
    while (isRunning) {
      val userId = random.nextInt(1000)
      val orderId = random.nextInt(100)
      val status = random.nextInt(1)
      val isNew = random.nextInt(1)
      val price = random.nextDouble()
      val quantity = new Random(10).nextInt()
      val order = SideEntry(userId, orderId, siteId = 1, cityId = 1, status, isNew, price, quantity, System.currentTimeMillis)
      ctx.collect(order)
    }
  }

}

