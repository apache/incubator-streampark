package com.streamxhub.spark.core.sink

import java.util.Properties

import org.apache.hadoop.hbase.client.{ConnectionFactory, HTable}
import org.apache.hadoop.hbase.{HBaseConfiguration, KeyValue}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.{HFileOutputFormat2, LoadIncrementalHFiles}
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

/**
  *
  * @deprecated 通过文件的方式批量加载数据
  */
@deprecated
class HFileSink[T: ClassTag](@transient override val sc: SparkContext,
                             val convert: RDD[T] => RDD[(ImmutableBytesWritable, KeyValue)],
                             val initParams: Map[String, String] = Map.empty[String, String]) extends Sink[T] {

  override val paramPrefix: String = "spark.sink.hfile."

  private lazy val prop = {
    val p = new Properties()
    p.putAll(param ++ initParams)
    p
  }

  private val tableName = prop.getProperty("hbase.table")


  /** 输出
    *
    * @param rdd  初始化HFileSink时指定类型的RDD
    * @param time spark.streaming.Time
    */
  override def output(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.foreachPartition { i =>
      val hconfig = HBaseConfiguration.create()
      prop.foreach { case (k, v) => hconfig.set(k, v) }

      convert(rdd).saveAsNewAPIHadoopFile("/tmp/hfile_sink_tmp",
        classOf[ImmutableBytesWritable],
        classOf[KeyValue],
        classOf[HFileOutputFormat2],
        hconfig)
      val connect = ConnectionFactory.createConnection(hconfig)
      val table = connect.getTable(TableName.valueOf(tableName))
      val bulkLoader = new LoadIncrementalHFiles(hconfig)
      val admin = connect.getAdmin
      val location = connect.getRegionLocator(table.getName)
      bulkLoader.doBulkLoad(new Path("/tmp/hfile_sink_tmp"), admin, table, location)
    }
  }


}
