package com.streamxhub.spark.monitor.support.actor

import MySQLActor._
import com.streamxhub.spark.monitor.support.mysql.MySQLClient
import com.streamxhub.spark.monitor.support.mysql.MySQLClient

/**
  * Created by benjobs on 2019/01/09.
  *
  */

class MySQLActor extends BaseActor {
  override def receive: Receive = {
    case Select(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive Select($db),$sql")
      val result = MySQLClient.select(sql, db)
      currentSender ! result
    case SelectOne(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive SelectOne($db),$sql")
      val result = MySQLClient.selectOne(sql, db)
      currentSender ! result
    case ExecuteUpdate(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive ExecuteUpdate($db),$sql")
      val result = MySQLClient.executeUpdate(sql, db)
      currentSender ! result
    case ExecuteBatch(sql, db) =>
      val currentSender = sender()
      logger.info(s"receive ExecuteBatch($db),$sql")
      val result = MySQLClient.executeBatch(sql, db)
      currentSender ! result
  }
}

object MySQLActor {

  case class Select(sql: String, db: String = "")

  case class SelectOne(sql: String, db: String = "")

  case class ExecuteUpdate(sql: String, db: String = "")

  case class ExecuteBatch(sql: Iterable[String], db: String = "")

}
