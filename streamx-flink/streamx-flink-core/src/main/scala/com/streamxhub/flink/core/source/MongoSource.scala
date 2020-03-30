package com.streamxhub.flink.core.source

import java.util.Properties

import com.streamxhub.common.util.{JdbcUtils, Logger}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.Map



class MongoSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * @param sqlFun
   * @param fun
   * @param interval sql查询的间隔时间.
   * @param config
   * @tparam R
   * @return
   */
  def getDataStream[R: TypeInformation](sqlFun: => String, fun: List[Map[String, _]] => List[R], interval: Long = 1000L)(implicit config: Properties): DataStream[R] = {
    val mysqlFun = new MongoSourceFunction[R](sqlFun, fun, interval)
    ctx.addSource(mysqlFun)
  }

}

/**
 *
 * @param sqlFun
 * @param resultFun
 * @param interval 两次sql执行查询的间隔.防止死循环密集的查询sql
 * @param typeInformation$R$0
 * @param config
 * @tparam R
 */
private[this] class MongoSourceFunction[R: TypeInformation](sqlFun: => String, resultFun: List[Map[String, _]] => List[R], interval: Long)(implicit config: Properties) extends RichSourceFunction[R] with Logger {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  override def open(parameters: Configuration): Unit = {


  }

  @throws[Exception]
  override def run(@(transient@param) ctx: SourceFunction.SourceContext[R]): Unit = {
    while (true) {
      resultFun(JdbcUtils.select(sqlFun)).foreach(ctx.collect)
      Thread.sleep(interval)
    }
  }

}