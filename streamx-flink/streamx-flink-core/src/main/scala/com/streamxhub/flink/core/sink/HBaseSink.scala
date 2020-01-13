package com.streamxhub.flink.core.sink

import java.util.Properties

import com.streamxhub.common.util.Logger
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.util.FlinkConfigUtils
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client._
import com.streamxhub.common.conf.ConfigConst._

import scala.collection.JavaConversions._
import scala.collection.{Map, mutable}

object HBaseSink {

  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): HBaseSink = new HBaseSink(ctx, overwriteParams, parallelism, name, uid)

}

class HBaseSink(@transient ctx: StreamingContext,
                overwriteParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null)(implicit instance: String = "") extends Sink with Logger {

  /**
   * @param stream
   * @param tableName
   * @param fun
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T], tableName: String)(implicit fun: T => Mutation): DataStreamSink[T] = {
    val prop = FlinkConfigUtils.get(ctx.parameter, HBASE_PREFIX)(instance)
    overwriteParams.foreach { case (k, v) => prop.put(k, v) }
    val sinkFun = new HBaseSinkFunction[T](prop, tableName, fun)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}

class HBaseSinkFunction[T](prop: Properties, tabName: String, fun: T => Mutation) extends RichSinkFunction[T] with Logger {

  private var connection: Connection = _
  private var table: Table = _
  private var mutator: BufferedMutator = _
  private var count: Int = 0

  private val commitBatch = prop.getOrElse(KEY_HBASE_COMMIT_BATCH, "1000").toInt
  private val mutations = new mutable.ArrayBuffer[Mutation]()

  override def open(parameters: Configuration): Unit = {
    val conf = HBaseConfiguration.create
    prop.foreach(x => conf.set(x._1, x._2))
    connection = ConnectionFactory.createConnection(conf)
    val tableName = TableName.valueOf(tabName)
    mutator = connection.getBufferedMutator(new BufferedMutatorParams(tableName))
    table = connection.getTable(tableName)
    count = 0
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    fun(value) match {
      case put: Put => mutator.mutate(put)
      case other => mutations += other
    }
    if (count % commitBatch == 0) {
      val start = System.currentTimeMillis()
      mutator.flush()
      if (mutations.nonEmpty) {
        table.batch(mutations, new Array[AnyRef](mutations.length))
        mutations.clear()
        logInfo(s"[StreamX] HBaseSink batch ${mutations.size} use ${System.currentTimeMillis() - start} MS")
      }
    }
    count += 1
  }

  override def close(): Unit = {
    if (mutator != null) {
      mutator.flush()
      mutator.close()
    }
    if (table != null) {
      table.close()
    }
    if (connection != null) {
      connection.close()
    }
  }

}