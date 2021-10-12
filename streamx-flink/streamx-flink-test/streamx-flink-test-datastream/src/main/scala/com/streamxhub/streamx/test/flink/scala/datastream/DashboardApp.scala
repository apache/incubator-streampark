/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.test.flink.scala.datastream

import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.source.SourceFunction

import scala.collection.JavaConverters._
import scala.util.Random

/**
 * 电商实时大屏Dashboard...
 */
object DashboardApp extends FlinkStreaming {

  def doAction(x: OrderEntry): java.util.List[String] = {
    List(x.userId.toString, x.siteId.toString).asJava
  }

  override def handle(): Unit = {
    val source = context.addSource(new OrderSource())
    val ds = source.map(x => doAction(x)).flatMap(_.asScala)
    ds.print()
  }

}

/**
 *
 * @param userId      : 用户Id
 * @param orderId     : 订单ID
 * @param siteId      : 站点ID
 * @param siteName    :站点名称
 * @param cityId      : 城市Id
 * @param cityName    : 城市名称
 * @param orderStatus : 订单状态(1:下单,0:退单)
 * @param isNewOrder  : 是否是首单
 * @param price       : 单价
 * @param quantity    : 订单数量
 * @param timestamp   : 下单时间
 */
case class OrderEntry(userId: Long,
                      orderId: Long,
                      siteId: Long,
                      siteName: String,
                      cityId: Long,
                      cityName: String,
                      orderStatus: Int,
                      isNewOrder: Int,
                      price: Double,
                      quantity: Int,
                      timestamp: Long)

class OrderSource extends SourceFunction[OrderEntry] {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()

  override def run(ctx: SourceFunction.SourceContext[OrderEntry]): Unit = {
    while (isRunning) {
      val userId = random.nextInt(1000)
      val orderId = random.nextInt(100)
      val status = random.nextInt(1)
      val isNew = random.nextInt(1)
      val price = random.nextDouble()
      val quantity = new Random(10).nextInt()
      val order = OrderEntry(userId, orderId, siteId = 1, siteName = "", cityId = 1, cityName = "", status, isNew, price, quantity, System.currentTimeMillis)
      ctx.collect(order)
    }
  }

}

