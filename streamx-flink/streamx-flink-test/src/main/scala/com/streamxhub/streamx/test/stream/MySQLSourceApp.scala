package com.streamxhub.streamx.test.stream

import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.source.JdbcSource
import org.apache.flink.api.scala._


object MySQLSourceApp extends FlinkStreaming {

  override def handle(): Unit = {

    JdbcSource().getDataStream[Order](lastOne => {
      val laseOffset = if (lastOne == null) "2020-10-10 23:00:00" else lastOne.timestamp
      s"select * from t_order where timestamp > '$laseOffset' order by timestamp asc "
    },
      _.map(x => new Order(x("market_id").toString, x("timestamp").toString))
    ).print()

  }

}

class Order(val marketId: String, val timestamp: String) extends Serializable
