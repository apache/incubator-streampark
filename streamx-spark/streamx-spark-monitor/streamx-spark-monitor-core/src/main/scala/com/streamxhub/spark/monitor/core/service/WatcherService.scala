package com.streamxhub.spark.monitor.core.service

import java.io.StringReader
import java.util.Properties

import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import com.streamxhub.spark.monitor.api.Const._
import com.streamxhub.spark.monitor.api.util.PropertiesUtil

import scala.collection.JavaConversions._
import com.streamxhub.spark.monitor.core.domain.{SparkConf, _}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Base64Utils

import scala.language.postfixOps


@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Array(classOf[Exception]))
class WatcherService(@Autowired sparkConfService: SparkConfService) {

  def config(id: String, conf: String): Unit = {
    val confMap = getConfigMap(conf)
    val appName = confMap(SPARK_PARAM_APP_NAME)
    val confVersion = confMap(SPARK_PARAM_APP_CONF_LOCAL_VERSION)
    val sparkConf = new SparkConf(appName, confVersion, Base64Utils.encodeToString(conf.getBytes))
    sparkConf.confId = id.toInt
    sparkConfService.config(sparkConf) match {
      case 0 => println("插入或更新失败....")
      case _ => println("插入或更新成功....")
    }
    System.out.println(id + ":config")
  }

  def publish(id: String): Unit = {
    System.out.println(id + ":start")
  }

  def shutdown(id: String): Unit = {
    System.out.println(id + ":shutdown")
  }

  private[this] def getConfigMap(conf: String): Map[String, String] = {
    if (!conf.matches(SPARK_CONF_REGEXP)) PropertiesUtil.getPropertiesFromYamlText(conf).toMap else {
      val properties = new Properties()
      properties.load(new StringReader(conf))
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    }
  }


}
