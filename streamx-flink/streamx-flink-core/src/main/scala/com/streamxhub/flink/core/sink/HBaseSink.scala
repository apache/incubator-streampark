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
import org.apache.hadoop.hbase.client.{BufferedMutator, BufferedMutatorParams, Connection, ConnectionFactory, Put, Table}
import com.streamxhub.common.conf.ConfigConst._

import scala.collection.JavaConversions._
import scala.collection.Map

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
  def sink[T](stream: DataStream[T], tableName: String)(implicit fun: T => Put): DataStreamSink[T] = {
    val prop = FlinkConfigUtils.get(ctx.parameter, HBASE_PREFIX)(instance)
    overwriteParams.foreach(x => prop.put(x._1, x._2))
    val sinkFun = new HBaseSinkFunction[T](prop, tableName, fun)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}

class HBaseSinkFunction[T](prop: Properties, tabName: String, fun: T => Put) extends RichSinkFunction[T] with Logger {

  var conn: Connection = _
  var mutator: BufferedMutator = _
  var count = 0

  override def open(parameters: Configuration): Unit = {
    val conf = HBaseConfiguration.create
    prop.foreach(x => conf.set(x._1, x._2))
    conn = ConnectionFactory.createConnection(conf)
    val tableName = TableName.valueOf(tabName)
    mutator = conn.getBufferedMutator(new BufferedMutatorParams(tableName))
    count = 0
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    val put = fun(value)
    mutator.mutate(put)
    if (count >= 2000) {
      mutator.flush()
      count = 0
    }
    count += 1
  }

  override def close(): Unit = {
    if (mutator != null) {
      mutator.flush()
      mutator.close()
    }
    if (conn != null) {
      conn.close()
    }
  }
}
