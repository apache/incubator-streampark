/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.spark.connector.kafka.offset

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import scalikejdbc.{ConnectionPool, DB, _}

/**
 * MySQL Offset Manager
 *
 * The table model for storing offsets is as follow, (topic+groupId+partition as union unique
 * primary key)
 *
 * | ---------------------------------------------------------------------------------- |             |           |        |
 * |:-----------------------------------------------------------------------------------|:------------|:----------|:-------|
 * | topic                                                                              | groupId     | partition | offset |
 * | ---------------------------------------------------------------------------------- |             |           |        |
 * | topic_001                                                                          | groupId_001 | 0         | 197    |
 * | ---------------------------------------------------------------------------------- |             |           |        |
 * | topic_001                                                                          | groupId_001 | 1         | 200    |
 * | ---------------------------------------------------------------------------------- |             |           |        |
 */
private[kafka] class MySQLOffset(val sparkConf: SparkConf) extends Offset {

  private lazy val jdbcURL = storeParams("mysql.jdbc.url")
  private lazy val table = storeParams("mysql.table")
  private lazy val user = storeParams("mysql.user")
  private lazy val password = storeParams("mysql.password")

  /**
   * get stored offset
   *
   * @param groupId
   * @param topics
   * @return
   */
  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    require(topics.nonEmpty)
    ConnectionPool.singleton(jdbcURL, user, password)
    DB.getTable(table) match {
      case None =>
        DB.autoCommit {
          implicit session =>
            val sql =
              s"""
                 |create table if not exists $table (
                 |`topic` varchar(255),
                 |`groupId` varchar(255),
                 |`partition` varchar(255),
                 |`offset` int(10),
                 |UNIQUE INDEX `INX`(`topic`, `groupId`,`partition`)
                 |)
          """.stripMargin
            SQL(sql).execute.apply()
        }
        logWarn(s"storeType:MySQL,table: $table is not exist,auto created...")
        Map.empty[TopicPartition, Long]
      case Some(_) =>
        DB.readOnly {
          implicit session =>
            val where = topics.size match {
              case 1 => s""" `topic` = "${topics.head}"  """
              case _ =>
                s""" `topic` in (${topics.mkString("\"", "\",\"", "\"")}) """
            }
            val sql =
              s"select `topic`,`partition`,`offset` from $table where `groupId`=? and $where"
            SQL(sql)
              .bind(groupId)
              .map {
                result =>
                  new TopicPartition(result.string(1), result.int(2)) -> result
                    .long(3)
              }
              .list
              .apply()
              .toMap
        }
    }
  }

  /**
   * update offset
   *
   * @param groupId
   * @param offsetInfos
   */
  override def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit = {
    DB.localTx {
      implicit session =>
        offsetInfos.foreach {
          case (tp, offset) =>
            val sql =
              s"insert into $table(`topic`,`groupId`,`partition`,`offset`) values(?,?,?,?) on duplicate key update `offset`= values(`offset`) "
            val updated = SQL(sql)
              .bind(tp.topic(), groupId, tp.partition(), offset)
              .update()
            if (updated == 0) {
              throw new Exception(s"Commit kafka topic :${tp.topic()} failed!")
            }
            logInfo(s"storeType:MySQL,updateOffsets [ $groupId,${offsetInfos.mkString(",")} ]")
        }
    }
  }

  /**
   * delete offset
   *
   * @param groupId
   * @param topics
   */
  override def delete(groupId: String, topics: Set[String]): Unit = {
    DB.autoCommit {
      implicit session =>
        topics.foreach(topic => {
          val sql = "delete from $table where topic=? and groupId=?"
          SQL(sql).bind(topic, groupId).update()
        })
    }
    logInfo(s"storeType:MySQL,deleteOffsets [ $groupId,${topics.mkString(",")} ]")
  }
}
