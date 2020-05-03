package com.streamxhub.flink.core.source

import java.util.Properties

import com.streamxhub.common.util.{HBaseClient, Logger}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.flink.streaming.api.functions.source.RichSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.TableName
import scala.collection.JavaConverters._
import scala.annotation.meta.param
import scala.collection.immutable.Map

object HBaseSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): HBaseSource = new HBaseSource(ctx, overrideParams)

}

/*
 * @param ctx
 * @param overrideParams
 */
class HBaseSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String], interval: Long = 0) {

  def getDataStream[R: TypeInformation](table: String, query: List[Query], fun: Result => R)(implicit config: Properties): DataStream[R] = {
    overrideParams.foreach(x => config.setProperty(x._1, x._2))
    val hbaseFun = new HBaseSourceFunction[R](table, query, fun, interval)
    ctx.addSource(hbaseFun)
  }

}


class HBaseSourceFunction[R: TypeInformation](table: String, query: List[Query], fun: Result => R, interval: Long)(implicit prop: Properties) extends RichSourceFunction[R] with Serializable with Logger {

  private[this] var isRunning = true

  private var htable: Table = null

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    htable = HBaseClient(prop).connection.getTable(TableName.valueOf(table))
  }

  override def run(ctx: SourceContext[R]): Unit = {
    query match {
      case scan: List[Scan] =>
        while (isRunning) {
          scan.foreach(x => htable.getScanner(x).iterator().asScala.toList.foreach(x => ctx.collect(fun(x))))
          Thread.sleep(interval)
        }
      case get: List[Get] =>
        while (isRunning) {
          htable.get(get.asInstanceOf[java.util.List[Get]]).toList.foreach(x => ctx.collect(fun(x)))
          Thread.sleep(interval)
        }
      case _ =>
        throw new IllegalArgumentException("[StreamX-Flink] HBaseSource error! query must Get or Scan!")
    }
  }

  override def cancel(): Unit = this.isRunning = false

  override def close(): Unit = {
    if (htable != null) {
      htable.close()
    }
  }
}
