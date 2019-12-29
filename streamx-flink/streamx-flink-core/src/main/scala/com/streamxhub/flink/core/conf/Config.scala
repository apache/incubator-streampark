package com.streamxhub.flink.core.conf

import java.util.Properties

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.Map
import scala.util.Try
import scala.collection.JavaConversions._
import ConfigConst._

object Config {

  def getKafkaSink(parameter: ParameterTool, topic: String): Properties = kafkaGet(parameter, SINK_KAFKA_PREFIX, topic)

  def getKafkaSource(parameter: ParameterTool, topic: String): Properties = kafkaGet(parameter, SOURCE_KAFKA_PREFIX, topic)

  private[this] def kafkaGet(parameter: ParameterTool, prefix: String, _topic: String): Properties = {
    val param: Map[String, String] = filterParam(parameter, prefix)
    if (param.isEmpty) throw new IllegalArgumentException(s"${_topic} init error...") else {
      val kafkaProperty = new Properties()
      kafkaProperty.putAll(param)
      val topic = _topic match {
        case SIGN_EMPTY =>
          val top = kafkaProperty.getProperty(TOPIC, null)
          if (top == null || top.split(SIGN_COMMA).length > 1) {
            throw new IllegalArgumentException("topic error...")
          } else top
        case t => t
      }

      val prefix = kafkaProperty.toMap.filter(_._1.startsWith(TOPIC)).filter(_._2.split(SIGN_COMMA).toSet.contains(topic)).keys.map(_.drop(5))
      if (prefix.size > 1) {
        throw new IllegalArgumentException(s"Can't find a unique topic of:${topic}!!!")
      } else {
        val fix = prefix.head
        val prop = new Properties()
        kafkaProperty.filter(x => x._1.endsWith(fix)).map(x => prop.put(x._1.dropRight(fix.length), x._2))
        prop
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
      case (_, _, x, y) if (x != null && y == null) || (x != null && y != null) => throw new IllegalArgumentException(s"MySQL Source instance:${prefix} error, [user|password] must be all null,or all not null ")
      case _ =>
    }
    val param: Map[String, String] = filterParam(parameter, fix)
    val properties = new Properties()
    properties.put(KEY_MYSQL_DRIVER, driver)
    properties.putAll(param)
    properties
  }

  private[this] def filterParam(parameter: ParameterTool, fix: String): Map[String, String] = {
    parameter.toMap.filter(x => x._1.startsWith(fix) && Try(x._2.nonEmpty).getOrElse(false)).flatMap(x => Some(x._1.substring(fix.length) -> x._2)).toMap
  }

}
