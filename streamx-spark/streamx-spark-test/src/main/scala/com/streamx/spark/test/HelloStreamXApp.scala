package com.streamx.spark.test

import org.apache.spark.streaming.StreamingContext
import com.streamxhub.spark.core.XStreaming
import com.streamxhub.spark.core.source.KafkaDirectSource
import org.apache.spark.SparkConf

object HelloStreamXApp extends XStreaming {

  /**
    * 用户设置sparkConf参数,如,spark序列化:
    * conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    * // 注册要序列化的自定义类型。
    * conf.registerKryoClasses(Array(classOf[User], classOf[Order],...))
    *
    * @param conf
    */
  override def configure(conf: SparkConf): Unit = {}


  override def handle(ssc: StreamingContext): Unit = {

    val source = new KafkaDirectSource[String, String](ssc)
    source.getDStream[(String, String)](x => (x.topic, x.value)).foreachRDD((rdd, time) => {

      //处理逻辑
      rdd.map(_._2).foreach(println)

      //提交offset
      source.updateOffset(time.milliseconds)
    })
  }

}
