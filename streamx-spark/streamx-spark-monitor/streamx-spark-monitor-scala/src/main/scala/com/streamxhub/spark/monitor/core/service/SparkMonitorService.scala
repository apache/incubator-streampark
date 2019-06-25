package com.streamxhub.spark.monitor.core.service

import com.streamxhub.spark.monitor.api.Const._
import com.streamxhub.spark.monitor.core.dao.SparkMonitorDao
import com.streamxhub.spark.monitor.core.domain.SparkMonitor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service class SparkMonitorService @Autowired()(sparkMonitorDao: SparkMonitorDao) {

  def publish(id: String, confMap: Map[String, String]): Unit = {
    doAction(id, 1, confMap)
  }

  def shutdown(id: String, confMap: Map[String, String]): Unit = {
    doAction(id, 0, confMap)
  }

  private[this] def doAction(id: String, status: Int, confMap: Map[String, String]): Unit = {
    val appName = confMap(SPARK_PARAM_APP_NAME)
    val confVersion = confMap(SPARK_PARAM_APP_CONF_LOCAL_VERSION)
    val appId = confMap(SPARK_PARAM_APP_ID)
    val monitor = new SparkMonitor(id.toInt,appId, appName, confVersion)
    monitor.status = status
    sparkMonitorDao.get(id) match {
      case null => sparkMonitorDao.save(monitor)
      case _ => sparkMonitorDao.update(monitor)
    }
  }

}
