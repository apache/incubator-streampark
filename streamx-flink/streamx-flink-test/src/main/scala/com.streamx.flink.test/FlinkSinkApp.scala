package com.streamxhub.flink.test


import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.sink.{KafkaSink, Mapper, RedisSink}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization

object FlinkSinkApp extends FlinkStreaming {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def handler(context: StreamingContext): Unit = {

    val source = context.env.readTextFile("data/in/persion.txt")

    val ds = source.flatMap(x => {
      x.split(",") match {
        case Array(d, a, b, c) => Some(Persion(d.toInt, a, b.toInt, c.toInt))
        case _ => None
      }
    })

    val ds1 = ds.map(x => Serialization.write(x))

    //Kafka sink..................
    //1)定义 KafkaSink
    val kafkaSink = new KafkaSink(context)
    //2)下沉数据. done
    kafkaSink.sink(ds1)

    val ds2 = source.flatMap(x => {
      x.split(",") match {
        case Array(d, a, b, c) => Some(User(d.toInt, a, b.toInt, c.toInt))
        case _ => None
      }
    })

    // Redis sink..................
    //1)定义 RedisSink
    val sink = RedisSink(context)
    //2)写Mapper映射
    implicit val mapper = Mapper[Persion](RedisCommand.HSET, "flink_persion", _.id.toString, _.name)
    implicit val mapper1 = Mapper[User](RedisCommand.HSET, "flink_user", _.id.toString, _.name)

    //3)下沉数据.done
    sink.sink[User](ds2)
    sink.sink[Persion](ds)

  }


}


case class Persion(id: Int, name: String, sex: Int, age: Int)

case class User(id: Int, name: String, sex: Int, age: Int)