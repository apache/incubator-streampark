package com.streamxhub.flink.test


import com.streamxhub.flink.core.{StreamingContext, XStreaming}
import com.streamxhub.flink.core.sink.{KafkaSink, Mapper, RedisSink}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization

object FlinkSinkApp extends XStreaming {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def handler(context: StreamingContext): Unit = {

    //1)读取数据源
    val source = context.env.readTextFile("data/in/person.txt")

    val ds = source.flatMap(x => {
      x.split(",") match {
        case Array(d, a, b, c) => Some(Person(d.toInt, a, b.toInt, c.toInt))
        case _ => None
      }
    })

    val ds1 = ds.map(x => Serialization.write(x))

    //Kafka sink..................
    //1)定义 KafkaSink
    val kfkSink = new KafkaSink(context)
    //2)下沉到目标
    kfkSink.sink(ds1)


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
    implicit val personMapper: Mapper[Person] = Mapper[Person](RedisCommand.HSET, "flink_person", _.id.toString, _.name)
    implicit val userMapper: Mapper[User] = Mapper[User](RedisCommand.HSET, "flink_user", _.id.toString, _.name)

    //3)下沉数据.done
    sink.sink[User](ds2)
    sink.sink[Person](ds)

  }


}


case class Person(id: Int, name: String, sex: Int, age: Int)

case class User(id: Int, name: String, sex: Int, age: Int)