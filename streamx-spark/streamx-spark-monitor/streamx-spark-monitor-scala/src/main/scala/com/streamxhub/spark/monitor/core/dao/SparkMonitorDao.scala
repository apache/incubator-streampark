package com.streamxhub.spark.monitor.core.dao

import com.streamxhub.spark.monitor.core.domain.SparkMonitor
import com.streamxhub.spark.monitor.core.utils.MySQLClient
import org.springframework.stereotype.Repository
import scala.language.postfixOps

@Repository class SparkMonitorDao {

  def get(id: String): SparkMonitor = {
    val sql = s"select * from t_spark_monitor where monitor_id=$id"
    MySQLClient selectOne[SparkMonitor] sql
  }

  def save(x: SparkMonitor): Int = {
    val sql =
      s"""
         |  insert into t_spark_monitor(monitor_id,app_id,app_name,conf_version,status,create_time)
         |  value(${x.monitorId},'${x.appId}','${x.appName}','${x.confVersion}',${x.status},now())
      """.stripMargin
    MySQLClient executeUpdate sql
  }

  def update(x: SparkMonitor): Int = {
    val sql =
      s"""
         |  update t_spark_monitor
         |  set app_name='${x.appName}',app_id='${x.appId}',conf_version='${x.confVersion}',status=${x.status},modify_time=now()
         |  where monitor_id=${x.monitorId}
      """.stripMargin
    MySQLClient executeUpdate sql
  }


}

