package com.streamxhub.spark.core

import com.streamxhub.spark.core.util.Utils
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer
/**
  *
  */
trait Spark {

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
  def init(sparkConf: SparkConf): Unit = {}

  /**
    * spark 启动后 调用
    */
  def afterStarted(sc: SparkContext): Unit = {}

  /**
    * spark 停止后 程序结束前 调用
    */
  def beforeStop(sc: SparkContext): Unit = {}

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

    init(sparkConf)

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