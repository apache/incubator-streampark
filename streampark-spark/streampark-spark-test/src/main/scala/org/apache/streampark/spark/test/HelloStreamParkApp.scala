/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.spark.test

import org.apache.streampark.spark.connector.kafka.source.KafkaSource
import org.apache.streampark.spark.core.SparkStreaming
import scalikejdbc.{ConnectionPool, DB, SQL}

object HelloStreamParkApp extends SparkStreaming {

  override def handle(): Unit = {

    val jdbcURL = sparkConf.get("spark.sink.mysql.jdbc.url")
    val user = sparkConf.get("spark.sink.mysql.user")
    val password = sparkConf.get("spark.sink.mysql.password")

    val source = new KafkaSource[String, String](context)

    val line = source.getDStream[String](x => (x.value))

    line.flatMap(_.split(" ")).map(_ -> 1).reduceByKey(_ + _)
      .foreachRDD((rdd, time) => {

        // handle transform
        rdd.foreachPartition(iter => {

          // sink data to MySQL
          ConnectionPool.singleton(jdbcURL, user, password)

          DB.autoCommit { implicit session =>
            val sql =
              s"""
                 |create table if not exists word_count (
                 |`word` varchar(255),
                 |`count` int(255),
                 |UNIQUE INDEX `INX`(`word`)
                 |)
                    """.stripMargin
            SQL(sql).execute.apply()
          }

          DB.localTx(implicit session => {
            iter.foreach(x => {
              val sql = s"replace into word_count(`word`,`count`) values('${x._1}',${x._2})"
              SQL(sql).update()
            })
          })
        })
        // commit offset
        source.updateOffset(time)
      })
  }
}
