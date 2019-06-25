package com.streamxhub.spark.monitor.core.dao

import com.streamxhub.spark.monitor.core.domain.SparkConf
import org.springframework.stereotype.Repository
import com.streamxhub.spark.monitor.core.utils.MySQLClient

import scala.language.postfixOps

@Repository class SparkConfDao {


  def save(x: SparkConf): Int = {
    val sql =
      s"""
         |  INSERT INTO T_SPARK_CONF(ID,APP_NAME,CONF_VERSION,CONF,CREATE_TIME)
         |  VALUE(${x.confId},'${x.appName}','${x.confVersion}','${x.conf}',now())
      """.stripMargin
    MySQLClient executeUpdate sql
  }

  def update(x: SparkConf): Int = {
    val sql =
      s"""
         |  UPDATE T_SPARK_CONF
         |  SET APP_NAME='${x.appName}',CONF_VERSION='${x.confVersion}',CONF='${x.conf}',MODIFY_TIME=now()
         |  WHERE ID=${x.confId}
      """.stripMargin
    MySQLClient executeUpdate sql
  }

  def get(confId: Int): SparkConf = {
    val sql = s"SELECT * FROM T_SPARK_CONF WHERE ID=$confId"
    MySQLClient selectOne[SparkConf] sql
  }

  def saveRecord(x: SparkConf): Int = {
    val sql =
      s"""
         |  INSERT INTO T_SPARK_CONF_HISTORY(CONF_ID,APP_NAME,CONF_VERSION,CONF,CREATE_TIME)
         |  VALUE(${x.confId},'${x.appName}','${x.confVersion}','${x.conf}','${x.createTime}')
      """.stripMargin
    MySQLClient executeUpdate sql
  }

}

