package com.streamxhub.spark.core.source

import com.streamxhub.spark.core.util.Logger
import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import scala.annotation.meta.getter
import scala.reflect.ClassTag
import scala.util.Try

/**
  *
  *
  * 源
  */
trait Source extends Logger with Serializable {
  //  lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  @(transient@getter)
  val ssc: StreamingContext
  @(transient@getter)
  lazy val sparkConf: SparkConf = ssc.sparkContext.getConf

  val prefix: String

  lazy val param: Map[String, String] = sparkConf.getAll.flatMap {
    case (k, v) if k.startsWith(prefix) && Try(v.nonEmpty).getOrElse(false) =>
      Some(k.substring(prefix.length) -> v)
    case _ => None
  } toMap


  type SourceType

  /**
    * 获取DStream 流
    *
    * @return
    */
  def getDStream[R: ClassTag](f: SourceType => R): DStream[R]
}
