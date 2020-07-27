package com.streamxhub.flink.test

import java.sql.PreparedStatement

import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcSink, JdbcStatementBuilder}
import com.streamxhub.flink.core.source.KafkaSource
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object JdbcSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    //one topic
    val source = new KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })

    //what fuck....
    source.addSink(JdbcSink.sink[String](
      "insert into books (id, title, author, price, qty) values (?,?,?,?,?)",
      new JdbcStatementBuilder[String]() {
        override def accept(t: PreparedStatement, u: String): Unit = {
          //.......
        }
      },
      new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl("localhost")
        .withDriverName("com.mysql.jdbc.Driver")
        .build()))


  }

}


