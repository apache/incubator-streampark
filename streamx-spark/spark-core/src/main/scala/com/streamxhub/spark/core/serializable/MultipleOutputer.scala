package com.streamxhub.spark.core.serializable

import org.apache.avro.mapreduce.AvroMultipleOutputs
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs

/**
  *
  */
/**
  * 多目录输出
  *
  * @tparam K
  * @tparam V
  */
trait MultipleOutputer[K, V] {
  def write(key: K, value: V, path: String): Unit

  def close(): Unit
}

object MultipleOutputer {

  /**
    * Avro 多文件输出
    *
    * @param mo
    * @tparam K
    * @tparam V
    */
  implicit class AvroMultipleOutputer[K, V](mo: AvroMultipleOutputs) extends MultipleOutputer[K, V] {
    def write(key: K, value: V, path: String): Unit = mo.write(key, value, path)

    def close(): Unit = mo.close()
  }

  /**
    * 无格式多路径输出
    *
    * @param mo
    * @tparam K
    * @tparam V
    */
  implicit class PlainMultipleOutputer[K, V](mo: MultipleOutputs[K, V]) extends MultipleOutputer[K, V] {
    def write(key: K, value: V, path: String): Unit = mo.write(key, value, path)

    def close(): Unit = mo.close()
  }

}
