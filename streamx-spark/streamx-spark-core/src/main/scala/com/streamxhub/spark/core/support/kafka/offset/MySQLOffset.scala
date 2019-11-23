package com.streamxhub.spark.core.support.kafka.offset

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf


/**
  *
  *
  * MySQL 存储Offset
  */
private[kafka] class MySQLOffset(val sparkConf: SparkConf) extends Offset {
  /**
    * 获取存储的Offset
    *
    * @param groupId
    * @param topics
    * @return
    */
  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    Map.empty[TopicPartition, Long]
  }

  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsetInfos
    */
  override def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit = {

  }

  /**
    * 删除 Offsets
    *
    * @param groupId
    * @param topics
    */
  override def delete(groupId: String, topics: Set[String]): Unit = {

  }
}
