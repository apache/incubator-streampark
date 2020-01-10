package com.streamxhub.flink.test

import com.streamxhub.flink.core.sink.MySQLSink
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.util.Random

object MySQLSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val orderType = TypeInformation.of[OrderBean](classOf[OrderBean])
    val source = context.addSource(new OrderBeanSource())
    MySQLSink(context).sink[OrderBean](source)(x => {
      s"insert into orders(userId,orderId,siteId,cityId,orderStatus,price,quantity,timestamp) values(${x.userId},${x.orderId},${x.siteId},${x.cityId},${x.orderStatus},${x.price},${x.quantity},${x.timestamp})"
    })
  }

}

/**
 *
 * @param userId      : 用户Id
 * @param orderId     : 订单ID
 * @param siteId      : 站点ID
 * @param cityId      : 城市Id
 * @param orderStatus : 订单状态(1:下单,0:退单)
 * @param price       : 单价
 * @param quantity    : 订单数量
 * @param timestamp   : 下单时间
 */
case class OrderBean(userId: Long,
                     orderId: Long,
                     siteId: Long,
                     cityId: Long,
                     orderStatus: Int,
                     price: Double,
                     quantity: Int,
                     timestamp: Long)

class OrderBeanSource extends SourceFunction[OrderBean] {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()

  override def run(ctx: SourceFunction.SourceContext[OrderBean]): Unit = {
    while (isRunning) {
      val userId = random.nextInt(1000)
      val orderId = random.nextInt(100)
      val status = random.nextInt(1)
      val price = random.nextDouble()
      val quantity = new Random(10).nextInt()
      val order = OrderBean(userId, orderId, siteId = 1, cityId = 1, status, price, quantity, System.currentTimeMillis)
      ctx.collect(order)
    }
  }

}

