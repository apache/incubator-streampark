package com.streamxhub.spark.core.sink

import java.util.Properties

import com.streamxhub.spark.core.util.Utils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.reflect.ClassTag

/**
  *
  *
  * 暂不支持checkpoint模式
  */
class InfluxDBSink[T: ClassTag](@transient override val sc: SparkContext,
                                initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] {

  override val paramPrefix: String = "spark.sink.influxDB."

  private lazy val prop = {
    val p = new Properties()
    p.putAll(param ++ initParams)
    p
  }

  private val host = prop.getProperty("host")
  private val port = prop.getProperty("port", "8086")
  private val db = prop.getProperty("db", "influx")

  def output(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.foreach(d => {
      val (postData, ip, pt, dbName) = d match {
        case t: String => (t, host, port, db)
        case (k: String, v: String) =>
          val info = k.split(",")
          info.size match {
            case 1 => (v, info(0), port, db)
            case 2 => (v, info(0), info(1), db)
            case 3 => (v, info(0), info(1), info(2))
            case _ => (v, host, port, db)
          }
        case (h: String, p: String, d: String, v: String) => (v, h, p, d)
        case _ => (d.toString, host, port, db)
      }
      val (code, res) = Utils.httpPost(s"http://$ip:$pt/write?db=$dbName", postData)
      code match {
        case d if d >= 200 && d < 300 =>
          logger.info(s"Write influxDB successful. $code")
        case _ =>
          logger.warn(s"Write influxDB failed. code: $code, res: $res")
      }
    })
  }

}

object InfluxDBSink {
  def apply(sc: SparkContext) = new InfluxDBSink[String](sc)

  def mkInsertData(table: String, dimension: Map[String, String], values: Map[String, Any]) = {
    val ds = dimension.map(d => s"${d._1}=${d._2}").mkString(",")
    val vs = values.map(d => s"${d._1}=${d._2}").mkString(",")
    s"$table,$ds $vs"
  }
}
