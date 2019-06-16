package com.streamxhub.spark.core.sink

import java.util.Properties

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time
import scala.collection.JavaConversions._

/**
  *
  */
class ShowSink[T](@transient override val sc: SparkContext,
                  initParams: Map[String, String] = Map.empty[String, String]) extends Sink[T] {

  override val prefix: String = "spark.sink.show."

  private lazy val prop = {
    val p = new Properties()
    p.putAll(param ++ initParams)
    p
  }

  private val num = prop.getProperty("num", "10").toInt


  /**
    * 输出
    *
    */
  override def output(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    val firstNum = rdd.take(num + 1)
    println("-------------------------------------------")
    println("Time: " + time)
    println("-------------------------------------------")
    firstNum.take(num).foreach(println)
    if (firstNum.length > num) println("...")
    println()
  }
}
