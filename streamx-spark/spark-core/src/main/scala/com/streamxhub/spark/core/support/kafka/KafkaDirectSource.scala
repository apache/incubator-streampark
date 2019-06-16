package com.streamxhub.spark.core.support.kafka

import java.util.concurrent.ConcurrentHashMap

import com.streamxhub.spark.core.source.Source
import com.streamxhub.spark.core.support.kafka.manager.KafkaManager
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, OffsetRange}

import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.Try

/**
  *
  * 封装 Kafka Direct Api
  *
  * @param ssc
  * @param specialKafkaParams 指定 Kafka 配置,可以覆盖配置文件
  */
class KafkaDirectSource[K: ClassTag, V: ClassTag](@transient val ssc: StreamingContext,
                                                  specialKafkaParams: Map[String, String] = Map.empty[String, String]
                                                 ) extends Source {

  override val prefix: String = "spark.source.kafka.consume."

  // 保存 offset
  private lazy val offsetRanges: java.util.Map[Long, Array[OffsetRange]] = new ConcurrentHashMap[Long, Array[OffsetRange]]

  private var canCommitOffsets: CanCommitOffsets = _

  // 分区数
  lazy val repartition: Int = sparkConf.get("spark.source.kafka.consume.repartition", "0").toInt

  // kafka 消费 topic
  private lazy val topicSet: Set[String] = specialKafkaParams.getOrElse("consume.topics",
    sparkConf.get("spark.source.kafka.consume.topics")).split(",").map(_.trim).toSet

  // 组装 Kafka 参数
  private lazy val kafkaParams: Map[String, String] = {
    sparkConf.getAll.flatMap {
      case (k, v) if k.startsWith(prefix) && Try(v.nonEmpty).getOrElse(false) => Some(k.substring(prefix.length) -> v)
      case _ => None
    } toMap
  } ++ specialKafkaParams ++ Map("enable.auto.commit" -> "false")

  lazy val groupId = kafkaParams.get("group.id")

  val km = new KafkaManager(ssc.sparkContext.getConf)


  override type SourceType = ConsumerRecord[K, V]

  /**
    * 获取DStream 流
    *
    * @return
    */
  override def getDStream[R: ClassTag](messageHandler: ConsumerRecord[K, V] => R): DStream[R] = {
    val stream = km.createDirectStream[K, V](ssc, kafkaParams, topicSet)
    canCommitOffsets = stream.asInstanceOf[CanCommitOffsets]
    stream.transform((rdd, time) => {
      offsetRanges.put(time.milliseconds, rdd.asInstanceOf[HasOffsetRanges].offsetRanges)
      rdd
    }).map(messageHandler)
  }

  /**
    * 更新Offset 操作 一定要放在所有逻辑代码的最后
    * 这样才能保证,只有action执行成功后才更新offset
    */
  def updateOffsets(time: Long): Unit = {
    // 更新 offset
    if (groupId.isDefined) {
      logInfo(s"updateOffsets with ${km.offsetManagerType} for time $time offsetRanges: $offsetRanges")
      val offset = offsetRanges.get(time)
      km.offsetManagerType match {
        case "kafka" => canCommitOffsets.commitAsync(offset)
        case _ => km.updateOffsets(groupId.get, offset)
      }
    }
    offsetRanges.remove(time)
  }
}
