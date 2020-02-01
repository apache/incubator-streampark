package com.streamxhub.flink.test

import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.util.Random


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
case class TestEntity(userId: Long,
                      orderId: Long,
                      siteId: Long,
                      cityId: Long,
                      orderStatus: Int,
                      price: Double,
                      quantity: Int,
                      timestamp: Long)


class TestSource extends SourceFunction[TestEntity] {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()

  var index = 0

  override def run(ctx: SourceFunction.SourceContext[TestEntity]): Unit = {
    while (isRunning && index <= 10001) {
      index += 1
      val userId = System.currentTimeMillis()
      val orderId = random.nextInt(100)
      val status = random.nextInt(1)
      val price = random.nextDouble()
      val quantity = new Random().nextInt(10)
      val order = TestEntity(userId, orderId, siteId = 1, cityId = 1, status, price, quantity, System.currentTimeMillis)
      ctx.collect(order)
    }
  }

}