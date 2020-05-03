package com.streamxhub.flink.test

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.source.HBaseSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.java.typeutils.GenericTypeInfo
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.io.TimeRange

object HBaseSourceApp extends FlinkStreaming {

  override def config(env: StreamExecutionEnvironment, parameter: ParameterTool): Unit = {
    val mySerializer = new ScanSerializer()
    val typeInfo = new GenericTypeInfo(classOf[Scan])
    typeInfo.createSerializer(env.getConfig)
    env.getConfig.addDefaultKryoSerializer(classOf[TimeRange],mySerializer)
    env.getConfig.enableGenericTypes()
    env.getConfig.enableForceKryo()
  }

  override def handler(context: StreamingContext): Unit = {
    implicit val conf = ConfigUtils.getHBaseConfig(context.paramMap)
    val scan = new Scan()
    new HBaseSource(context).getDataStream[String]("test:user",List(scan), x=>{
      print(x)
      ""
    }).print()
  }

}

class ScanSerializer extends Serializer[TimeRange] with java.io.Serializable {
  override def write(kryo: Kryo, output: Output, value: TimeRange): Unit = {
    println(output)
    println(value)
  }

  override def read(kryo: Kryo, input: Input, `type`: Class[TimeRange]): TimeRange = {
    null
  }

}