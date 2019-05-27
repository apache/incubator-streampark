package com.streamxhub.spark.core.util

import scala.collection.Map
import scala.util.Try

/**
  * 默认配置
  */
trait Config {
  val config: Map[String, String] = Try {
    Utils.getPropertiesFromFile("src/main/resources/application.properties")
  } getOrElse Map.empty[String, String]
}
