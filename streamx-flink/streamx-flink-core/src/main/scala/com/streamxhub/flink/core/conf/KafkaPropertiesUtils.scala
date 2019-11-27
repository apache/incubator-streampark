package com.streamxhub.flink.core.conf

import java.util.Properties

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.Map
import scala.util.Try
import scala.collection.JavaConversions._
import Const._

object KafkaPropertiesUtils {

  def getSink(parameter: ParameterTool, topic: String): Properties = {
    val param: Map[String, String] = parameter.toMap.flatMap {
      case (k, v) if k.startsWith(SINK_KAFKA_PREFIX) && Try(v.nonEmpty).getOrElse(false) =>
        val param = k.substring(SINK_KAFKA_PREFIX.length)
        Some(param -> v)
      case _ => None
    }.toMap
    getProperties(param, topic)
  }

  def getSource(parameter: ParameterTool, topic: String): Properties = {
    val param: Map[String, String] = parameter.toMap.flatMap {
      case (k, v) if k.startsWith(SOURCE_KAFKA_PREFIX) && Try(v.nonEmpty).getOrElse(false) =>
        val param = k.substring(SOURCE_KAFKA_PREFIX.length)
        Some(param -> v)
      case _ => None
    }.toMap
    getProperties(param, topic)
  }

  private def getProperties(param: Map[String, String], _topic: String): Properties = {
    if (param.isEmpty) {
      throw new IllegalArgumentException(s"${_topic} init error...")
    } else {
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
        kafkaProperty.filter(x => x._1.endsWith(fix)).map(x => {
          prop.put(x._1.dropRight(fix.length), x._2)
        })
        prop
      }
    }
  }

}
