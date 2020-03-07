package com.streamxhub.flink.test

import com.streamxhub.flink.core.sink.{InfluxDBSink, InfluxEndpoint}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._

import scala.util.Random

/**
 * 侧输出流
 */
object InfluxDBSinkApp extends FlinkStreaming {


  override def handler(context: StreamingContext): Unit = {
    val source = context.addSource(new WeatherSource())

    //weather,altitude=1000,area=北 temperature=11,humidity=-4
    implicit val end = InfluxEndpoint[Weather](
      "mydb",
      "weather",
      "autogen",
      x => Map("altitude" -> x.altitude.toString, "area" -> x.area.toString),
      x => Map("temperature" -> x.temperature, "humidity" -> x.humidity))

    InfluxDBSink(context).sink(source)
  }

}

/**
 *
 * 温度 temperature
 * 湿度 humidity
 * 地区 area
 * 海拔 altitude
 */
case class Weather(temperature: Long,
                   humidity: Long,
                   area: String,
                   altitude: Long)

class WeatherSource extends SourceFunction[Weather] {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()

  override def run(ctx: SourceFunction.SourceContext[Weather]): Unit = {
    while (isRunning) {
      val temperature = random.nextInt(1000)
      val humidity = random.nextInt(30)
      val area = List("北","上","广","深")(random.nextInt(4))
      val altitude = random.nextInt(10000)
      val order = Weather(temperature, humidity, area, altitude)
      ctx.collect(order)
    }
  }

}

