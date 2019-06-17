package com.streamx.spark.test

import org.apache.spark.streaming.StreamingContext
import com.streamxhub.spark.core.XStreaming
import com.streamxhub.spark.core.support.kafka.KafkaDirectSource

object HelloStreamXApp extends XStreaming {
    override def handle(ssc : StreamingContext): Unit = {
        val source = new KafkaDirectSource[String,String](ssc)
        source.getDStream[(String,String)](x => (x.topic,x.value)).foreachRDD((rdd,time) => {
            rdd.take(10).foreach(println)
            source.updateOffsets(time.milliseconds)
        })
    }
}
