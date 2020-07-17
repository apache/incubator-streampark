package com.streamxhub.flink.test

import java.util

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.source.HBaseSource
import com.streamxhub.flink.core.wrapper.{HBaseGet, HBaseScan}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.util.Bytes
import org.apache.flink.api.scala._

object HBaseSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    implicit val conf = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    val get = new HBaseScan()
    new HBaseSource(context).getDataStream[String]("person", get, r => {
      val map = new util.HashMap[String, String]()
      val cellScanner = r.cellScanner()
      while (cellScanner.advance()) {
        val cell = cellScanner.current()
        val q = Bytes.toString(CellUtil.cloneQualifier(cell))
        val (name, v) = q.split("_") match {
          case Array(_type, name) =>
            _type match {
              case "i" => name -> Bytes.toInt(CellUtil.cloneValue(cell))
              case "s" => name -> Bytes.toString(CellUtil.cloneValue(cell))
              case "d" => name -> Bytes.toDouble(CellUtil.cloneValue(cell))
              case "f" => name -> Bytes.toFloat(CellUtil.cloneValue(cell))
            }
          case _ =>
        }
        map.put(name.toString, v.toString)
      }
      map.toString
    }).print("result--->")

  }

}
