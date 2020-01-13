package com.streamxhub.flink.core.util

import java.util.Properties

import org.apache.flink.api.java.utils.ParameterTool
import scala.collection.JavaConversions._
import com.streamxhub.common.conf.ConfigConst._
import org.apache.commons.lang3.StringUtils

import scala.collection.Map
import scala.util.Try

object FlinkConfigUtils {

  def get(parameter: ParameterTool, prefix: String = "")(instance: String = ""): Properties = {
    val map = filterParam(parameter, prefix + instance)
    val prop = new Properties()
    map.foreach { case (k, v) => prop.put(k, v) }
    prop
  }

  def getKafkaSink(parameter: ParameterTool, topic: String, instance: String = ""): Properties = kafkaGet(parameter, KAFKA_SINK_PREFIX + instance, topic)

  def getKafkaSource(parameter: ParameterTool, topic: String, instance: String = ""): Properties = kafkaGet(parameter, KAFKA_SOURCE_PREFIX + instance, topic)

  private[this] def kafkaGet(parameter: ParameterTool, prefix: String, inTopic: String): Properties = {
    val param: Map[String, String] = filterParam(parameter, if (prefix.endsWith(".")) prefix else s"${prefix}.")
    if (param.isEmpty) throw new IllegalArgumentException(s"${inTopic} init error...") else {
      val kafkaProperty = new Properties()
      param.foreach(x => kafkaProperty.put(x._1, x._2))
      val topic = inTopic match {
        case SIGN_EMPTY =>
          val top = kafkaProperty.getProperty(KEY_KAFKA_TOPIC, null)
          if (top == null || top.split(SIGN_COMMA).length > 1) {
            throw new IllegalArgumentException(s"Can't find a unique topic!!!")
          } else top
        case t => t
      }
      val hasTopic = !kafkaProperty.toMap.exists(x => x._1 == KEY_KAFKA_TOPIC && x._2.split(SIGN_COMMA).toSet.contains(topic))
      if (hasTopic) {
        throw new IllegalArgumentException(s"Can't find a topic of:${topic}!!!")
      } else {
        kafkaProperty.put(KEY_KAFKA_TOPIC, topic)
        kafkaProperty
      }
    }
  }

  def getMySQL(parameter: ParameterTool)(implicit prefix: String = ""): Properties = mysqlGet(parameter, MYSQL_PREFIX, prefix)

  private[this] def mysqlGet(parameter: ParameterTool, prefix: String, instance: String): Properties = {
    val tmpFix = if (instance == null || instance.isEmpty) prefix else s"${prefix}.${instance}"
    val fix = tmpFix.replaceAll("\\.+",".").replaceAll("\\.+$","").concat(".")
    val driver = parameter.toMap.getOrDefault(s"${prefix}${KEY_MYSQL_DRIVER}", null)
    val url = parameter.toMap.getOrDefault(s"${fix}${KEY_MYSQL_URL}", null)
    val user = parameter.toMap.getOrDefault(s"${fix}${KEY_MYSQL_USER}", null)
    val password = parameter.toMap.getOrDefault(s"${fix}${KEY_MYSQL_PASSWORD}", null)

    (driver, url, user, password) match {
      case (x, y, _, _) if x == null || y == null => throw new IllegalArgumentException(s"MySQL instance:${prefix} error,[driver|url] must be not null")
      case (_, _, x, y) if (x != null && y == null) || (x == null && y != null) => throw new IllegalArgumentException(s"MySQL instance:${prefix} error, [user|password] must be all null,or all not null ")
      case _ =>
    }
    val param: Map[String, String] = filterParam(parameter, fix)
    val properties = new Properties()
    val instanceName = if (StringUtils.isBlank(instance)) "default" else instance
    properties.put(KEY_MYSQL_INSTANCE, instanceName)
    properties.put(KEY_MYSQL_DRIVER, driver)
    param.foreach(x => properties.put(x._1, x._2))
    properties
  }

  private[this] def filterParam(parameter: ParameterTool, fix: String): Map[String, String] = {
    parameter
      .toMap
      .filter(x => x._1.startsWith(fix) && Try(x._2.nonEmpty).getOrElse(false))
      .flatMap(x =>
        Some(x._1.substring(fix.length).replaceFirst("^\\.", "") -> x._2)
      ).toMap
  }

}
