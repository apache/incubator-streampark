package com.streamxhub.flink.test


import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.sink.KafkaSink
import com.streamxhub.flink.core.source.KafkaSource
import org.apache.flink.streaming.api.scala._
import org.json4s.DefaultFormats

object FlinkSinkApp extends FlinkStreaming {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def handler(context: StreamingContext): Unit = {

    /**
     * 从kafka里读数据.这里的数据是数字或者字母,每次读取1条
     */
    val source = new KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })

    //Kafka sink..................
    //1)定义 KafkaSink
    val kfkSink = KafkaSink(context)
    //2)下沉到目标
    kfkSink.sink(source).setParallelism(1)

    /*val ds2 = source.flatMap(x => {
      x.split(",") match {
        case Array(d, a, b, c) => Some(User(d.toInt, a, b.toInt, c.toInt))
        case _ => None
      }
    })

    // Redis sink..................
    //1)定义 RedisSink
    val sink = RedisSink(context)
    //2)写Mapper映射
    val personMapper: Mapper[Person] = Mapper[Person](RedisCommand.HSET, "flink_person", _.id.toString, _.name)
    val userMapper: Mapper[User] = Mapper[User](RedisCommand.HSET, "flink_user", _.id.toString, _.name)

    //3)下沉数据.done
    sink.sink[User](ds2,userMapper)
    sink.sink[Person](ds,personMapper)*/

  }


}


case class Person(id: Int, name: String, sex: Int, age: Int)

case class User(id: Int, name: String, sex: Int, age: Int)