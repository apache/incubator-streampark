package com.streamxhub.flink.test

import com.streamxhub.flink.core.sink.HBaseSink
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import java.util.Random

object HBaseSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val orderType = TypeInformation.of[TestEntity](classOf[TestEntity])
    val source = context.addSource(new TestSource)
    val random = new Random()

    HBaseSink(context).sink[TestEntity](source, "order")(x => {
      val put = new Put(Bytes.toBytes(System.nanoTime()+random.nextInt(1000000)),x.timestamp)
      put.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("cid"),Bytes.toBytes(x.cityId))
      put.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("oid"),Bytes.toBytes(x.orderId))
      put.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("os"),Bytes.toBytes(x.orderStatus))
      put.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("oq"),Bytes.toBytes(x.quantity))
      put.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("sid"),Bytes.toBytes(x.siteId))
      put
    })

  }

}
