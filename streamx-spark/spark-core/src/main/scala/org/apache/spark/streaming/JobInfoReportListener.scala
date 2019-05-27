package org.apache.spark.streaming

//
import java.text.SimpleDateFormat
import java.util.{Properties, UUID}

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.spark.streaming.kafka010.OffsetRange
import com.streamxhub.spark.core.support.kafka.writer.KafkaWriter._
import com.streamxhub.spark.core.util.{Logger, Config}
import org.apache.spark.streaming.scheduler.{BatchInfo, StreamingListener, StreamingListenerBatchCompleted, StreamingListenerBatchStarted}

import scala.collection.mutable


/**
  *
  * 上报Job批次处理信息
  */
class JobInfoReportListener(ssc: StreamingContext) extends StreamingListener with Config with Logger {

  // Queue containing latest completed batches
  private val batchInfos = new mutable.Queue[BatchInfo]()

  private val producerConf = new Properties()
  producerConf.put("bootstrap.servers", ssc.conf.getOption("spark.monitor.kafka.bootstrap.servers").getOrElse(""))
  producerConf.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  producerConf.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")


  private val sinkTopic = ssc.conf.getOption("spark.monitor.kafka.topic")

  private val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  private val name = ssc.sparkContext.appName
  private val master = ssc.sparkContext.master
  private val batchDuration = ssc.graph.batchDuration.milliseconds
  private val appId = ssc.sparkContext.applicationId

  /**
    * 批处理计算完成
    *
    * @param batchStarted
    */
  override def onBatchCompleted(batchStarted: StreamingListenerBatchCompleted): Unit = {

    val batchInfo = batchStarted.batchInfo

    val offsetRanges = batchInfo.streamIdToInputInfo.values.flatMap(_.metadata("offsets").asInstanceOf[List[OffsetRange]])

    val offsetMap = mutable.Map.empty[String, Long]

    for (or <- offsetRanges) {
      offsetMap.put(or.topic, offsetMap.getOrElse(or.topic, 0L) + (or.untilOffset - or.fromOffset))
    }

    val batchTime = batchStarted.batchInfo.batchTime.milliseconds
    val schedulingDelay = batchStarted.batchInfo.schedulingDelay.getOrElse(0L)
    val processingDelay = batchStarted.batchInfo.processingDelay.getOrElse(0L)

    val time = sdf.format(batchTime)
    val sparkAppInfos = offsetMap.map {
      case (topic, numRecords) =>
        SparkAppInfo(name, master, batchDuration, appId, topic, time, numRecords, schedulingDelay, processingDelay, "completed")
    }
    if (sinkTopic.nonEmpty)
      sparkAppInfos.toIterator.writeToKafka(producerConf, x => new ProducerRecord[String, String](sinkTopic.get, UUID.randomUUID().toString, x.toString))

  }

  /**
    * 批处理计算开始
    *
    * @param batchSubmitted
    */
  override def onBatchStarted(batchSubmitted: StreamingListenerBatchStarted): Unit = {

    val batchInfo = batchSubmitted.batchInfo

    val offsetRanges = batchInfo.streamIdToInputInfo.values.flatMap(_.metadata("offsets").asInstanceOf[List[OffsetRange]])

    val offsetMap = mutable.Map.empty[String, Long]

    for (or <- offsetRanges) {
      offsetMap.put(or.topic, offsetMap.getOrElse(or.topic, 0L) + (or.untilOffset - or.fromOffset))
    }

    val batchTime = batchInfo.batchTime.milliseconds
    val schedulingDelay = batchInfo.schedulingDelay.getOrElse(0L)
    val time = sdf.format(batchTime)
    val sparkAppInfos = offsetMap.map {
      case (topic, numRecords) =>
        SparkAppInfo(name, master, batchDuration, appId, topic, time, numRecords, schedulingDelay, 0L, "started")
    }
    if (sinkTopic.nonEmpty)
      sparkAppInfos.toIterator.writeToKafka(producerConf, x => new ProducerRecord[String, String](sinkTopic.get, UUID.randomUUID().toString, x.toString))

  }
}

/**
  *
  * @param name
  * @param master
  * @param batchDuration
  * @param appId
  * @param topic
  * @param batchTime
  * @param numRecords
  * @param schedulingDelay
  * @param startedOrCompleted
  */
case class SparkAppInfo(name: String, master: String, batchDuration: Long, appId: String, topic: String, batchTime: String, numRecords: Long, schedulingDelay: Long, processingDelay: Long, startedOrCompleted: String) {
  override def toString: String = {
    s"""{"name":"$name","master":"$master","batchDuration":$batchDuration,"appId":"$appId","topic":"$topic","batchTime":"$batchTime","numRecords":$numRecords,"schedulingDelay":$schedulingDelay,"processingDelay":$processingDelay,"startedOrCompleted":"$startedOrCompleted"}"""
  }
}


