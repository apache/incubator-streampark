/**
  * Copyright (c) 2019 The StreamX Project
  * <p>
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  * <p>
  * http://www.apache.org/licenses/LICENSE-2.0
  * <p>
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */

package com.streamxhub.spark.core.support.kafka.offset

import java.util

import com.streamxhub.spark.core.support.hbase.HBaseClient
import org.apache.hadoop.hbase.{CellUtil, HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.{Delete, Put, Scan, Table}
import org.apache.hadoop.hbase.filter._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  *
  *
  * Hbase 存储Offset
  */
private[kafka] class HBaseOffset(val sparkConf: SparkConf) extends Offset {

  private lazy val tableName = storeParams("hbase.table")
  private lazy val familyName = storeParams.getOrElse("hbase.table.family", "tpo")
  private lazy val familyNameBytes = Bytes.toBytes(familyName)
  private lazy val topicBytes = Bytes.toBytes("topic")
  private lazy val partitionBytes = Bytes.toBytes("partition")
  private lazy val offsetBytes = Bytes.toBytes("offset")

  @transient
  private lazy val table: Table = {
    val conn = HBaseClient.connect(storeParams)
    if (!conn.getAdmin.tableExists(TableName.valueOf(tableName))) {
      val tableDesc: HTableDescriptor = new HTableDescriptor(TableName.valueOf(tableName))
      tableDesc.addFamily(new HColumnDescriptor(familyName))
      conn.getAdmin.createTable(tableDesc)
    }
    conn.getTable(TableName.valueOf(tableName))
  }


  /** 存放offset的表模型如下，请自行优化和扩展，请把每个rowkey对应的record的version设置为1（默认值），因为要覆盖原来保存的offset，而不是产生多个版本
    * ----------------------------------------------------------------------------------------------------
    * rowKey            |  column family                                                          |
    * --------------------------------------------------------------------------
    * |                 |  column:topic(string)  |  column:partition(int)  | column:offset(long)  |
    * ----------------------------------------------------------------------------------------------
    * topic#partition   |   topic                |   partition             |    offset            |
    * ---------------------------------------------------------------------------------------------------
    */

  /**
    * 获取存储的Offset
    *
    * @param groupId
    * @param topics
    * @return
    */
  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    val storedOffsetMap = new mutable.HashMap[TopicPartition, Long]()
    val earliestOffsets = getEarliestOffsets(topics.toSeq)

    topics.foreach(topic => {
      val filter = new PrefixFilter(key(groupId, topic).getBytes)
      val scan = new Scan().setFilter(filter)
      val result = table.getScanner(scan)

      result.foreach(r => {
        var topic = ""
        var partition = 0
        var offset = 0L
        while (r.advance()) {
          val cell = r.current()
          Bytes.toString(CellUtil.cloneQualifier(cell)) match {
            case "topic" => topic = Bytes.toString(CellUtil.cloneValue(cell))
            case "partition" => partition = Bytes.toInt(CellUtil.cloneValue(cell))
            case "offset" => offset = Bytes.toLong(CellUtil.cloneValue(cell))
            case _ =>
          }
        }
        // 如果Offset失效了，则用 earliestOffsets 替代
        val topicPartition = new TopicPartition(topic, partition)
        val finalOffset = earliestOffsets.get(topicPartition) match {
          case Some(left) if left > offset =>
            logWarning(s"consumer group:$groupId,topic:${topicPartition.topic},partition:${topicPartition.partition} offsets已经过时，更新为: $left")
            left
          case _ => offset
        }
        storedOffsetMap += topicPartition -> finalOffset
      })
      result.close()
    })

    // fix bug
    // 如果GroupId 已经在Hbase存在了，这个时候新加一个topic ，则新加的Topic 不会被消费
    val offsetMaps = reset.toLowerCase() match {
      case "latest" => getLatestOffsets(topics.toSeq) ++ storedOffsetMap
      case _ => getEarliestOffsets(topics.toSeq) ++ storedOffsetMap
    }

    logInfo(s"getOffsets [$groupId,${offsetMaps.mkString(",")}] ")

    offsetMaps
  }

  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsetInfos
    */
  override def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit = {
    val puts = offsetInfos.map {
      case (tp, offset) =>
        val put: Put = new Put(Bytes.toBytes(s"${key(groupId, tp.topic)}#${tp.partition}"))
        put.addColumn(familyNameBytes, topicBytes, Bytes.toBytes(tp.topic))
        put.addColumn(familyNameBytes, partitionBytes, Bytes.toBytes(tp.partition))
        put.addColumn(familyNameBytes, offsetBytes, Bytes.toBytes(offset))
        put
    }.toList
    table.put(puts)
    logInfo(s"updateOffsets [ $groupId,${offsetInfos.mkString(",")} ]")
  }

  /**
    * 删除Offset
    *
    * @param groupId
    * @param topics
    */
  override def delete(groupId: String, topics: Set[String]): Unit = {

    val filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE)

    topics.foreach(topic => {
      val filter = new RowFilter(CompareFilter.CompareOp.EQUAL, new BinaryPrefixComparator(Bytes.toBytes(s"${key(groupId, topic)}#")))
      filterList.addFilter(filter)
    })

    val scan = new Scan()
    scan.setFilter(filterList)

    val rs = table.getScanner(scan)
    val iter = rs.iterator()

    val deletes = new util.ArrayList[Delete]()
    while (iter.hasNext) {
      val r = iter.next()
      deletes.add(new Delete(Bytes.toBytes(new String(r.getRow))))
    }
    rs.close()
    table.delete(deletes)
    logInfo(s"deleteOffsets [ $groupId,${topics.mkString(",")} ] ${deletes.mkString(" ")}")
  }
}
