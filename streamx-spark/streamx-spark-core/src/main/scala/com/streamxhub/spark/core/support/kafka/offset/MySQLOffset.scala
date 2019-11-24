package com.streamxhub.spark.core.support.kafka.offset

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import scalikejdbc.{ConnectionPool, DB}
import scalikejdbc._

/**
  *
  *
  * MySQL 存储Offset
  */
private[kafka] class MySQLOffset(val sparkConf: SparkConf) extends Offset {

  private lazy val jdbcURL = storeParams("mysql.jdbc.url")
  private lazy val table = storeParams("mysql.table")
  private lazy val user = storeParams("mysql.user")
  private lazy val password = storeParams("mysql.password")

  /**
    * 获取存储的Offset
    *
    * @param groupId
    * @param topics
    * @return
    */
  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    require(topics.nonEmpty)
    //在Driver端创建数据库连接池
    ConnectionPool.singleton(jdbcURL, user, password)
    DB.getTable(table) match {
      case None =>
        DB.autoCommit { implicit session =>
          val sql =
            s"""
               |create table $table (
               |`topic` varchar(255),
               |`groupId` varchar(255),
               |`partition` varchar(255),
               |`offset` int(10),
               |UNIQUE INDEX `INX`(`topic`, `groupId`,`partition`)
               |)
          """.stripMargin
          SQL(sql).execute.apply()
        }
        Map.empty[TopicPartition, Long]
      case Some(_) =>
        DB.readOnly { implicit session =>
          val where = topics.size match {
            case 1 => s""" `topic` = "${topics.head}"  """
            case _ => s""" `topic` in (${topics.mkString("\"", "\",\"", "\"")}) """
          }
          val sql = s"select `topic`,`partition`,`offset` from $table where `groupId`=? and $where"
          SQL(sql).bind(groupId).map { result =>
            new TopicPartition(result.string(1), result.int(2)) -> result.long(3)
          }.list.apply().toMap
        }
    }
  }

  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsetInfos
    */
  override def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit = {
    DB.localTx { implicit session =>
      offsetInfos.foreach { case (tp, offset) =>
        val sql = s"replace into $table(`topic`,`groupId`,`partition`,`offset`) values(?,?,?,?)"
        val updated = SQL(sql).bind(tp.topic(), groupId, tp.partition(), offset).update().apply()
        if (updated != 1) {
          throw new Exception(s"Commit kafka topic :${tp.topic()} failed!")
        }
      }
    }
  }

  /**
    * 删除 Offsets
    *
    * @param groupId
    * @param topics
    */
  override def delete(groupId: String, topics: Set[String]): Unit = {
    DB.autoCommit { implicit session =>
      topics.foreach(topic => {
        val sql = "delete from $table where topic=? and groupId=?"
        SQL(sql).bind(topic, groupId).update().apply()
      })
    }
  }
}
