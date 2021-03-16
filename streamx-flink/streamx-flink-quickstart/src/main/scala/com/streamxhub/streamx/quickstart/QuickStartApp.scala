package com.streamxhub.streamx.quickstart

import com.streamxhub.streamx.common.util.JsonUtils
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.JdbcSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import org.apache.flink.api.scala._

object QuickStartApp extends FlinkStreaming {

  /**
   * 假如我们用从kafka里读取用户的数据写入到mysql中,需求如下<br/>
   *
   * <strong>1.从kafka读取用户数据写入到mysql</strong>
   *
   * <strong>2. 只要年龄小于30岁的数据</strong>
   *
   * <strong>3. kafka数据格式如下:</strong>
   * {
   * "name": "benjobs",
   * "age":  "28",
   * "gender":   "1",
   * "address":  "beijing"
   * }
   *
   * <strong>4. mysql表DDL如下:</strong>
   * create table user(
   * `name` varchar(32),
   * `age` int(3),
   * `gender` int(1),
   * `address` varchar(255)
   * )
   */
  override def handle(): Unit = {
    val source = KafkaSource()
      .getDataStream[String]()
      .map(x => JsonUtils.read[User](x.value))
      .filter(_.age < 30)

    JdbcSink().towPCSink[User](source)(user =>
      s"""
        |insert into t_user(`name`,`age`,`gender`,`address`)
        |value('${user.name}',${user.age},${user.gender},'${user.address}')
        |""".stripMargin
    )
  }

}

case class User(name: String, age: Int, gender: Int, address: String)

