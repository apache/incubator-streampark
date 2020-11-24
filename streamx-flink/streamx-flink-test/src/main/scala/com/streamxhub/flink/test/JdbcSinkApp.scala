package com.streamxhub.flink.test

import java.sql.PreparedStatement
import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcStatementBuilder, JdbcSink => JSink}
import com.streamxhub.flink.core.scala.sink.JdbcSink
import com.streamxhub.flink.core.scala.source.KafkaSource
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.streaming.api.scala._

object JdbcSinkApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {

    /**
     * 从kafka里读数据.这里的数据是数字或者字母,每次读取1条
     */
    val source = KafkaSource(context).getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })


    /**
     * 假设这里有一个orders表.有一个字段,id的类型可以是int
     * 在数据插入的时候制造异常:
     * 1)正确情况: 当从kafka中读取的内容全部是数字时会插入成功,kafka的消费的offset也会更新.
     * 如: 当前kafka size为20,手动输入10个数字,则size为30,然后会将这10个数字写入到Mysql,kafka的offset也会更新
     * 2)异常情况: 当从kafka中读取的内容非数字会导致插入失败,kafka的消费的offset会回滚
     * 如: 当前的kafka size为30,offset是30, 手动输入1个字母,此时size为31,写入mysql会报错,kafka的offset依旧是30,不会发生更新.
     */
    JdbcSink(context, parallelism = 5).towPCSink[String](source)(x => {
      s"insert into orders(id,timestamp) values('$x',${System.currentTimeMillis()})"
    }).uid("mysqlSink").name("mysqlSink")


    /**
     * what fuck....
     * 官方提供的jdbc
     */
    if (1 == 2) {
      source.addSink(JSink.sink[String](
        "insert into orders (id,timestamp) values (?,?)",
        new JdbcStatementBuilder[String]() {
          override def accept(t: PreparedStatement, u: String): Unit = {
            t.setString(1, u)
            t.setLong(2, System.currentTimeMillis())
          }
        },
        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
          .withUrl("jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true")
          .withDriverName("com.mysql.jdbc.Driver")
          .withUsername("root")
          .withPassword("123322242")
          .build()))

    }
  }

}


