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

package org.apache.streampark.flink.connector.kafka.bean

import org.apache.streampark.common.util.Logger
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner

import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.Nullable

/**
 *
 * <b>KafkaEqualityPartitioner</b>Equality Partitioner, the partitioner can evenly write data to each partition
 *
 * @param parallelism
 * @tparam T
 */
class KafkaEqualityPartitioner[T](parallelism: Int) extends FlinkKafkaPartitioner[T] with Logger {

  private[this] var parallelInstanceId = 0

  private[this] val partitionIndex: AtomicInteger = new AtomicInteger(0)

  override def open(parallelInstanceId: Int, parallelInstances: Int): Unit = {
    logInfo(s"KafkaEqualityPartitioner: parallelism $parallelism")
    require(parallelInstanceId >= 0 && parallelInstances > 0, "[StreamPark] KafkaEqualityPartitioner:Id of this subtask cannot be negative,Number of subtasks must be larger than 0.")
    this.parallelInstanceId = parallelInstanceId
  }

  override def partition(record: T, key: Array[Byte], value: Array[Byte], targetTopic: String, partitions: Array[Int]): Int = {
    require(partitions != null && partitions.length > 0, "[StreamPark] KafkaEqualityPartitioner:Partitions of the target topic is empty.")
    (parallelism, partitions.length) match {
      // kafka have 1 partition
      case (_, 1) => 0
      case (x, y) if x % y == 0 => partitions(parallelInstanceId % partitions.length)
      case (_, y) =>
        partitionIndex.get() match {
          case x if x == y - 1 => partitionIndex.getAndSet(0)
          case _ => partitionIndex.incrementAndGet()
        }
    }
  }

  override def equals(o: Any): Boolean = this == o || o.isInstanceOf[KafkaEqualityPartitioner[T]]

  override def hashCode: Int = classOf[KafkaEqualityPartitioner[T]].hashCode

  def checkArgument(condition: Boolean, @Nullable errorMessage: String): Unit = if (!condition) throw new IllegalArgumentException(errorMessage)

}
