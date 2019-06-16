package com.streamxhub.spark.core

import com.streamxhub.spark.core.util.{Heartbeat, Utils}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer
/**
  *
  */
trait XSpark {

  protected final def args: Array[String] = _args

  private final var _args: Array[String] = _

  private val sparkListeners = new ArrayBuffer[String]()

  @(transient@getter)
  var sparkSession: SparkSession = _

  /**
    * 初始化，函数，可以设置 sparkConf
    *
    * @param sparkConf
    */
  def initialize(sparkConf: SparkConf): Unit = {}

  /**
    * StreamingContext 运行之后执行
    */
  def afterStarted(sc: SparkContext): Unit = {
    Heartbeat(sc).start()
  }

  /**
    * StreamingContext 停止后 程序停止前 执行
    */
  def beforeStop(sc: SparkContext): Unit = {
    Heartbeat(sc).stop()
  }

  /**
    * 处理函数
    *
    * @param sc
    */
  def handle(sc: SparkContext)

  def creatingContext(): SparkContext = {
    val sparkConf = new SparkConf()

    sparkConf.set("spark.user.args", args.mkString("|"))

    // 约定传入此参数,则表示本地 Debug
    if (sparkConf.contains("spark.conf")) {
      sparkConf.setAll(Utils.getPropertiesFromFile(sparkConf.get("spark.conf")))
      sparkConf.setAppName("LocalDebug").setMaster("local[*]")
      sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10")
    }

    initialize(sparkConf)

    val extraListeners = sparkListeners.mkString(",") + "," + sparkConf.get("spark.extraListeners", "")
    if (extraListeners != "") sparkConf.set("spark.extraListeners", extraListeners)

    sparkSession = SparkSession.builder().enableHiveSupport().config(sparkConf).getOrCreate()

    val sc = sparkSession.sparkContext
    handle(sc)
    sc
  }

  def main(args: Array[String]): Unit = {

    this._args = args

    val context = creatingContext()
    afterStarted(context)
    context.stop()
    beforeStop(context)
  }

}
