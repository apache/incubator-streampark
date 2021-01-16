package com.streamxhub.streamx.spark.test

import com.streamxhub.streamx.spark.core.SparkStreaming
import com.streamxhub.streamx.spark.core.source.KafkaDirectSource
import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import scalikejdbc.{ConnectionPool, DB, SQL}

object HelloStreamXApp extends SparkStreaming {

  /**
   * 用户设置sparkConf参数,如,spark序列化:
   * conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
   * // 注册要序列化的自定义类型。
   * conf.registerKryoClasses(Array(classOf[User], classOf[Order],...))
   *
   * @param conf
   */
  override def configure(conf: SparkConf): Unit = {}


  override def handle(ssc: StreamingContext): Unit = {

    val sparkConf = ssc.sparkContext.getConf
    val jdbcURL = sparkConf.get("spark.sink.mysql.jdbc.url")
    val user = sparkConf.get("spark.sink.mysql.user")
    val password = sparkConf.get("spark.sink.mysql.password")

    val source = new KafkaDirectSource[String, String](ssc)

    source.getDStream[(String, String)](x => (x.topic, x.value)).foreachRDD((rdd, time) => {

      //transform 业务处理
      rdd.foreachPartition(iter => {

        //sink 数据落盘到MySQL
        ConnectionPool.singleton(jdbcURL, user, password)
        DB.localTx(implicit session => {
          iter.map(_._2.toInt).foreach(x => {
            val sql = s"insert into t_test(`key`) values($x)"
            SQL(sql).update().apply()
          })
        })
      })

      //提交offset
      source.updateOffset(time)

    })

  }

}
