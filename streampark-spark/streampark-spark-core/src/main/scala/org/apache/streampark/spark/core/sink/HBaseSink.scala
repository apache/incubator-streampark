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
//
//package org.apache.streampark.spark.core.sink
//
//import org.apache.streampark.common.util.HBaseClient
//import org.apache.hadoop.hbase.TableName
//import org.apache.hadoop.hbase.client._
//import org.apache.spark.SparkContext
//import org.apache.spark.rdd.RDD
//import org.apache.spark.streaming.Time
//
//import java.util.{ArrayList => JAList}
//import scala.collection.JavaConversions._
//import scala.reflect.ClassTag
//
///**
// *
// */
//class HBaseSink[T <: Mutation : ClassTag](@transient override val sc: SparkContext,
//                                          val initParams: Map[String, String] = Map.empty[String, String])
//  extends Sink[T] {
//
//  override val prefix: String = "spark.sink.hbase."
//
//  private lazy val prop = filterProp(param, initParams, prefix, "hbase.")
//
//  private val tableName = prop.getProperty("hbase.table")
//  private val commitBatch = prop.getProperty("hbase.commit.batch", "1000").toInt
//
//  private def getConnect: Connection = HBaseClient(prop).connection
//
//  private def getMutator: BufferedMutator = {
//    val connection = getConnect
//    val bufferedMutatorParams = new BufferedMutatorParams(TableName.valueOf(tableName))
//    connection.getBufferedMutator(bufferedMutatorParams)
//  }
//
//  private def getTable: Table = {
//    val connection = getConnect
//    connection.getTable(TableName.valueOf(tableName))
//  }
//
//  /** output
//   *
//   * @param rdd  RDD[Put]或者RDD[Delete]
//   * @param time spark.streaming.Time
//   */
//  override def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
//    rdd match {
//      case r: RDD[Put] => r.foreachPartition { put =>
//        val mutator = getMutator
//        put.foreach { p => mutator.mutate(p.asInstanceOf[Put]) }
//        mutator.flush()
//        mutator.close()
//      }
//
//      case r: RDD[Delete] => r.foreachPartition { del =>
//        val table = getTable
//        val delList = new JAList[Delete]()
//        del.foreach { d =>
//          delList += d.asInstanceOf[Delete]
//          if (delList.size() >= commitBatch) {
//            table.batch(delList, null)
//            delList.clear()
//          }
//        }
//        if (delList.size() > 0) {
//          table.batch(delList, null)
//          delList.clear()
//        }
//        table.close()
//      }
//    }
//  }
//}
//
//object HBaseSink {
//  def apply(sc: SparkContext): HBaseSink[Put] = new HBaseSink[Put](sc)
//
//  def apply[T <: Mutation : ClassTag](rdd: RDD[T]): HBaseSink[T] = new HBaseSink[T](rdd.sparkContext)
//}
