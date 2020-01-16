package com.streamxhub.flink.test

import com.streamxhub.flink.core.sink.ClickHouseSink
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

import collection.JavaConverters._
import scala.util.Random

/***
  * 建表语句
  * CREATE TABLE test.test (`name` String, `age` UInt16, `cnt` UInt16) ENGINE = TinyLog
  *
  * // clickhouse参数值
  * var schemaName: String = "test"
  * var tableName: String = "test"
  * var user: String = "default"
  * var password: String = ""
  * var url: String = "jdbc:clickhouse://xxx:8123"
  */
object ClickhouseSinkApp  {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val source =env.addSource(new SensorSource)
      .map( r =>Map(s"name"->r.name,s"age"->r.age,s"cnt"->r.cnt).asJava)

    source.addSink(new ClickHouseSink())

    env.execute("clickhouse sink test1")
  }

}

case class SensorReading1(name: String, age: Int, cnt: Int)

class SensorSource extends RichParallelSourceFunction[SensorReading1] {

  // flag indicating whether source is still running.
  var running: Boolean = true
  override def run(srcCtx: SourceContext[SensorReading1]): Unit = {
    val rand = new Random()
    val taskIdx = this.getRuntimeContext.getIndexOfThisSubtask
    var curFTemp = (1 to 10).map {
      i => ("sensor_" + (taskIdx * 10 + i), 65 + (rand.nextGaussian() * 20))
    }
    while (running) {
      curFTemp.foreach( t => srcCtx.collect(SensorReading1(t._1, 12, 23)))
      Thread.sleep(100)
    }
  }
  /** Cancels this SourceFunction. */
  override def cancel(): Unit = {
    running = false
  }

}
