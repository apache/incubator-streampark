package com.streamxhub.flink.test

import java.util.Properties

import com.streamxhub.common.util.JsonUtils
import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.flink.core.source.MySQLSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object MongoSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    //MongoSource()
  }

}

