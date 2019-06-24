package com.streamxhub.spark.monitor.core.dao

import akka.pattern._
import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.streamxhub.spark.monitor.core.actor.MySQLActor
import com.streamxhub.spark.monitor.core.domain.SparkConf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import com.streamxhub.spark.monitor.core.actor.MySQLActor._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

@Repository class SparkConfDao @Autowired()(implicit val system: ActorSystem) {


  implicit val timeout: Timeout = 10.minutes

  private lazy val mysqlActor = system.actorOf(Props[MySQLActor], "sparkConf-mysql-actor")

  def save(x: SparkConf): Future[Int] = {
    val sql =
      s"""
         |  INSERT INTO T_SPARK_CONF(ID,APP_NAME,CONF_VERSION,CONF,CREATE_TIME)
         |  VALUE(${x.confId},'${x.appName}','${x.confVersion}','${x.conf}',now())
      """.stripMargin
    (mysqlActor ? ExecuteUpdate(sql)).mapTo[Int]
  }

  def update(x: SparkConf): Future[Int] = {
    val sql =
      s"""
         |  UPDATE T_SPARK_CONF
         |  SET APP_NAME='${x.appName}',CONF_VERSION='${x.confVersion}',CONF='${x.conf}',MODIFY_TIME=now()
         |  WHERE ID=${x.confId}
      """.stripMargin
    (mysqlActor ? ExecuteUpdate(sql)).mapTo[Int]
  }

  def get(confId: Int): Future[Map[String, Any]] = {
    (mysqlActor ? SelectOne(s"SELECT * FROM T_SPARK_CONF WHERE ID=$confId")).mapTo[Map[String, Any]]
  }

  def saveRecord(x: SparkConf): Future[Int] = {
    val sql =
      s"""
         |  INSERT INTO T_SPARK_CONF_HISTORY(CONF_ID,APP_NAME,CONF_VERSION,CONF,CREATE_TIME)
         |  VALUE(${x.confId},'${x.appName}','${x.confVersion}','${x.conf}','${x.createTime}')
      """.stripMargin
    (mysqlActor ? ExecuteUpdate(sql)).mapTo[Int]
  }

}

