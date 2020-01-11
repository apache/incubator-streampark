package com.streamxhub.flink.core.conf

import java.util.Properties

import org.apache.flink.api.java.utils.ParameterTool
import com.streamxhub.common.util.MySQLUtils.{KEY_MYSQL_INSTANCE,KEY_MYSQL_DRIVER,KEY_MYSQL_PASSWORD,KEY_MYSQL_URL,KEY_MYSQL_USER}

import scala.collection.Map
import scala.util.Try
import scala.collection.JavaConversions._
import ConfigConst._
import org.apache.commons.lang3.StringUtils

object Config {

  def getKafkaSink(parameter: ParameterTool, topic: String, instance: String = ""): Properties = kafkaGet(parameter, SINK_KAFKA_PREFIX + instance, topic)

  def getKafkaSource(parameter: ParameterTool, topic: String, instance: String = ""): Properties = kafkaGet(parameter, SOURCE_KAFKA_PREFIX + instance, topic)

  private[this] def kafkaGet(parameter: ParameterTool, prefix: String, inTopic: String): Properties = {
    val param: Map[String, String] = filterParam(parameter, if (prefix.endsWith(".")) prefix else s"${prefix}.")
    if (param.isEmpty) throw new IllegalArgumentException(s"${inTopic} init error...") else {
      val kafkaProperty = new Properties()
      param.foreach(x=>kafkaProperty.put(x._1,x._2))
      val topic = inTopic match {
        case SIGN_EMPTY =>
          val top = kafkaProperty.getProperty(TOPIC, null)
          if (top == null || top.split(SIGN_COMMA).length > 1) {
            throw new IllegalArgumentException(s"Can't find a unique topic!!!")
          } else top
        case t => t
      }
      val hasTopic = !kafkaProperty.toMap.exists(x => x._1 == TOPIC && x._2.split(SIGN_COMMA).toSet.contains(topic))
      if (hasTopic) {
        throw new IllegalArgumentException(s"Can't find a topic of:${topic}!!!")
      } else {
        kafkaProperty.put(TOPIC, topic)
        kafkaProperty
      }
    }
  }

  def getMySQLSink(parameter: ParameterTool)(implicit prefix: String = ""): Properties = mysqlGet(parameter, SINK_MYSQL_PREFIX, prefix)

  def getMySQLSource(parameter: ParameterTool)(implicit prefix: String = ""): Properties = mysqlGet(parameter, SOURCE_MYSQL_PREFIX, prefix)

  private[this] def mysqlGet(parameter: ParameterTool, prefix: String, instance: String): Properties = {
    val fix = if (instance == null || instance.isEmpty) prefix else s"${prefix}.${instance}"
    val driver = parameter.toMap.getOrDefault(s"${prefix}.${KEY_MYSQL_DRIVER}", null)
    val url = parameter.toMap.getOrDefault(s"${fix}.${KEY_MYSQL_URL}", null)
    val user = parameter.toMap.getOrDefault(s"${fix}.${KEY_MYSQL_USER}", null)
    val password = parameter.toMap.getOrDefault(s"${fix}.${KEY_MYSQL_PASSWORD}", null)

    (driver, url, user, password) match {
      case (x, y, _, _) if x == null || y == null => throw new IllegalArgumentException(s"MySQL Source instance:${prefix} error,[driver|url] must be not null")
      case (_, _, x, y) if (x != null && y == null) || (x == null && y != null) => throw new IllegalArgumentException(s"MySQL Source instance:${prefix} error, [user|password] must be all null,or all not null ")
      case _ =>
    }
    val param: Map[String, String] = filterParam(parameter, fix)
    val properties = new Properties()
    val instanceName = if (StringUtils.isBlank(instance)) "default" else instance
    properties.put(KEY_MYSQL_INSTANCE, instanceName)
    properties.put(KEY_MYSQL_DRIVER, driver)
    param.foreach(x=>properties.put(x._1,x._2))
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
