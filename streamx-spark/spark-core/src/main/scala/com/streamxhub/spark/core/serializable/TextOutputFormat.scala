package com.streamxhub.spark.core.serializable

import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat

/**
  *
  *
  * 重载
  * generateFileNameForKeyValue 为 s"${key}_$name"
  * generateActualKey 为 null
  */
class TextOutputFormat extends MultipleTextOutputFormat[Any, Any] {

  override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String = {
    s"${key}_$name"
  }

  override def generateActualKey(key: Any, value: Any): AnyRef = {
    null
  }
}
