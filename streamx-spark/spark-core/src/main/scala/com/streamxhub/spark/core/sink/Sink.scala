package com.streamxhub.spark.core.sink

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.Time
import org.slf4j.LoggerFactory

import scala.annotation.meta.getter
import scala.util.Try

/**
  *
  */
trait Sink[T] extends Serializable {

  lazy val logger = LoggerFactory.getLogger(getClass)

  @(transient@getter)
  val sc: SparkContext
  @(transient@getter)
  lazy val sparkConf = sc.getConf

  val paramPrefix: String

  lazy val param: Map[String, String] = sparkConf.getAll.flatMap {
    case (k, v) if k.startsWith(paramPrefix) && Try(v.nonEmpty).getOrElse(false) => Some(k.substring(paramPrefix.length) -> v)
    case _ => None
  } toMap

  /**
    * 输出
    *
    */
  def output(dStream: DStream[T]): Unit = {
    dStream.foreachRDD((rdd, time) => output(rdd, time))
  }

  /**
    * 输出
    *
    * @param rdd  spark.RDD
    * @param time spark.streaming.Time
    */
  def output(rdd: RDD[T], time: Time)
}
