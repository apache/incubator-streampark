package com.streamxhub.flink.test

import java.text.SimpleDateFormat
import java.util.Date

import com.streamxhub.flink.core.{FLinkStreaming, StreamingContext}
import com.streamxhub.flink.core.sink.ElasticSearch6Sink
import com.streamxhub.flink.core.source.KafkaSource
import com.streamxhub.flink.core.util.{EsIndexUtils, WatermarkUtils}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.elasticsearch.action.index.IndexRequest
import org.json4s
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization

import scala.util.Try

object PolestarDashboardApp extends FLinkStreaming {

  /**
   * @param context
   */
  override def handler(context: StreamingContext): Unit = {
    val data = new KafkaSource(context)

    val ds = data.getDataStream().map(x => OrderEntity.build(x))
      .filter(_.ymd == now())
      .filter(_.gmv > 0)
      .keyBy(_.timestamp)
      .assignTimestampsAndWatermarks(WatermarkUtils.boundedOutOfOrdernessWatermark[OrderEntity](1000 * 30)(_.timestamp))
      .keyBy(_.client_id)
      .timeWindow(Time.seconds(60))
      .reduce(_ + _)

    implicit def indexReq(x: OrderEntity): IndexRequest = EsIndexUtils.indexRequest(
      s"polestar_dash_${x.ymd}",
      "_doc",
      s"${x.timestamp}",
      x.toJson
    )

    //数据下沉到es
    ElasticSearch6Sink(context).sink[OrderEntity](ds)

  }

  def now(fmt: String = "yyyyMMdd"): String = {
    val ymdFormat = new SimpleDateFormat(fmt)
    ymdFormat.format(new Date())
  }

}


case class OrderEntity(ymd: String, timestamp: Long, gmv: Double, profit: Double, client_id: String, num: Int = 1) {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  val secondFormat = new SimpleDateFormat("HH:mm")

  lazy val toJson: String = {
    val map = Map(
      "timestamp" -> new Date(this.timestamp),
      "ymd" -> this.ymd,
      "second" -> secondFormat.format(new Date(this.timestamp)),
      "num" -> this.num,
      "gmv" -> this.gmv,
      "profit" -> this.profit,
      "client_id" -> Try(this.client_id.toInt).getOrElse(0)
    )
    Serialization.write(map)
  }

  def +(OrderEntity: OrderEntity): OrderEntity = {
    this.copy(
      gmv = this.gmv + OrderEntity.gmv,
      profit = this.profit + OrderEntity.profit,
      num = this.num + OrderEntity.num
    )
  }

}

object OrderEntity {

  private val client_Map = Map("24224840" -> "2", "20902967" -> "3")

  def build(log: String): OrderEntity = {

    @transient
    implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

    val fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val ymdFormat = new SimpleDateFormat("yyyyMMdd")

    @transient
    lazy val json: json4s.JValue = parse(log)
    val create_time: String = (json \ "create_time").extractOrElse("1970-01-01 00:00:00")
    val timestamp = fullDateFormat.parse(create_time).getTime
    val ymd = ymdFormat.format(fullDateFormat.parse(create_time))

    val site_id: String = (json \ "site_id").extractOrElse("")
    val gmv: Double = (json \ "alipay_total_price").extractOpt[String].filter(_.nonEmpty).getOrElse("0").toDouble
    val profit: Double = (json \ "pub_share_pre_fee").extractOpt[String].filter(_.nonEmpty).getOrElse("0").toDouble
    val client_id: String = Try(client_Map(site_id)).getOrElse("")

    OrderEntity(ymd, timestamp, gmv, profit, client_id)
  }


}



