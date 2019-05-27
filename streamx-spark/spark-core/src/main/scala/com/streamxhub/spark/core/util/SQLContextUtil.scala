package com.streamxhub.spark.core.util

import org.apache.spark.SparkContext
import org.apache.spark.sql.{SQLContext, SparkSession}

/**
  *
  * SQLContext 单例
  */
object SQLContextUtil {

  @transient private var instance: SQLContext = _
  @transient private var hiveContext: SQLContext = _

  def getSqlContext(@transient sparkContext: SparkContext): SQLContext = {
    if (instance == null) {
      instance = SparkSession.builder().config(sparkContext.getConf).getOrCreate().sqlContext
    }
    instance
  }

  /**
    * 获取 HiveContext
    *
    * @param sparkContext
    * @return
    */
  def getHiveContext(@transient sparkContext: SparkContext): SQLContext = {
    if (hiveContext == null) {
      hiveContext = SparkSession.builder().config(sparkContext.getConf).enableHiveSupport().getOrCreate().sqlContext
    }
    hiveContext
  }
}
