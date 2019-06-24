package com.streamxhub.spark.monitor.core.actor

import MySQLActor._
import com.streamxhub.spark.monitor.core.utils.MySQLClient

import scala.reflect.ClassTag


class MySQLActor extends BaseActor {
  override def receive: Receive = {
    case Select(sql) =>
      val currentSender = sender()
      logger.info(s"receive Select: $sql")
      val result = MySQLClient.select(sql)
      currentSender ! result
    case SelectOne(sql) =>
      val currentSender = sender()
      logger.info(s"receive SelectOne:$sql")
      val result = MySQLClient.selectOne(sql)
      currentSender ! result
    case ExecuteUpdate(sql) =>
      val currentSender = sender()
      logger.info(s"receive ExecuteUpdate:$sql")
      val result = MySQLClient.executeUpdate(sql)
      currentSender ! result
    case ExecuteBatch(sql) =>
      val currentSender = sender()
      logger.info(s"receive ExecuteBatch,$sql")
      val result = MySQLClient.executeBatch(sql)
      currentSender ! result
  }
}

object MySQLActor {

  case class Select(sql: String)

  case class SelectOne(sql: String)

  case class ExecuteUpdate(sql: String)

  case class ExecuteBatch(sql: Iterable[String])

}
