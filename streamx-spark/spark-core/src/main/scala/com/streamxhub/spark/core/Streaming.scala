package com.streamxhub.spark.core

import com.streamxhub.spark.core.util.Utils
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.annotation.meta.getter
import scala.collection.mutable.ArrayBuffer

/**
  *
  * Spark Streaming 入口封装
  *
  */
trait Streaming {


  protected final def args: Array[String] = _args

  private final var _args: Array[String] = _

  private val sparkListeners = new ArrayBuffer[String]()

  // checkpoint目录
  private var checkpointPath: String = ""

  // 从checkpoint 中恢复失败，则重新创建
  private var createOnError: Boolean = true

  @(transient@getter)
  var sparkSession: SparkSession = _

  /**
    * 初始化，函数，可以设置 sparkConf
    *
    * @param sparkConf
    */
  def init(sparkConf: SparkConf): Unit = {}

  /**
    * StreamingContext 运行之前执行
    *
    * @param ssc
    */
  def beforeStarted(ssc: StreamingContext): Unit = {}

  /**
    * StreamingContext 运行之后执行
    */
  def afterStarted(ssc: StreamingContext): Unit = {}

  /**
    * StreamingContext 停止后 程序停止前 执行
    */
  def beforeStop(ssc: StreamingContext): Unit = {}

  /**
    * 处理函数
    *
    * @param ssc
    */
  def handle(ssc: StreamingContext)


  /**
    * 创建 Context
    *
    * @return
    */
  def creatingContext(): StreamingContext = {

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


    sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()

    // 时间间隔
    val slide = sparkConf.get("spark.batch.duration").toInt
    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(slide))
    handle(ssc)
    ssc
  }


  private def printUsageAndExit(): Unit = {

    System.err.println(
      """
        |"Usage: Streaming [options]
        |
        | Options are:
        |   --checkpointPath <checkpoint 目录设置>
        |   --createOnError <从 checkpoint 恢复失败,是否重新创建 true|false>
        |""".stripMargin)
    System.exit(1)
  }


  def main(args: Array[String]): Unit = {

    this._args = args

    var argv = args.toList

    while (argv.nonEmpty) {
      argv match {
        case ("--checkpointPath") :: value :: tail =>
          checkpointPath = value
          argv = tail
        case ("--createOnError") :: value :: tail =>
          createOnError = value.toBoolean
          argv = tail
        case Nil =>
        case tail =>
          System.err.println(s"Unrecognized options: ${tail.mkString(" ")}")
          printUsageAndExit()
      }
    }


    val context = checkpointPath match {
      case "" => creatingContext()
      case ck =>
        val ssc = StreamingContext.getOrCreate(ck, creatingContext, createOnError = createOnError)
        ssc.checkpoint(ck)
        ssc
    }
    beforeStarted(context)
    context.start()
    afterStarted(context)
    context.awaitTermination()
    beforeStop(context)
  }
}
