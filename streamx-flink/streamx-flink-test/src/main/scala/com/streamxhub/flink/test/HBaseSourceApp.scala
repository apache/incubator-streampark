package com.streamxhub.flink.test

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.source.HBaseSource
import com.streamxhub.flink.core.wrapper.{HBaseGet, HBaseScan}
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object HBaseSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val conf = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    val get = new HBaseScan
    new HBaseSource(context).getDataStream[String]("person",List(get), x=>{
      print(x)
      ""
    }).print()
  }

}
