package com.streamxhub.flink.test

import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import com.streamxhub.flink.core.sink.ClickHouseSink
import org.apache.flink.api.common.typeinfo.TypeInformation

/** *
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
object ClickhouseSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {
    implicit val entryType = TypeInformation.of[TestEntity](classOf[TestEntity])
    val source = context.addSource(new TestSource)
    ClickHouseSink(context).sink[TestEntity](source)(x => {
      s"insert into orders(userId,orderId,siteId,cityId,orderStatus,price,quantity,timestamp) values(${x.userId},${x.orderId},${x.siteId},${x.cityId},${x.orderStatus},${x.price},${x.quantity},${x.timestamp})"
    })
  }

}

