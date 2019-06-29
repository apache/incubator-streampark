package com.streamx.spark.test

import org.apache.spark.streaming.StreamingContext
import com.streamxhub.spark.core.XStreaming
import com.streamxhub.spark.core.support.kafka.KafkaDirectSource
import org.apache.spark.SparkConf

object HelloStreamXApp extends XStreaming {


  /**
    * 初始化，函数，可以设置 sparkConf
    *
    * @param sparkConf
    */

  override def handle(ssc: StreamingContext): Unit = {
    val source = new KafkaDirectSource[String, String](ssc)
    source.getDStream[(String, String)](x => (x.topic, x.value)).foreachRDD((rdd, time) => {
      rdd.take(10).foreach(println)
      source.updateOffsets(time.milliseconds)
    })
  }

}
