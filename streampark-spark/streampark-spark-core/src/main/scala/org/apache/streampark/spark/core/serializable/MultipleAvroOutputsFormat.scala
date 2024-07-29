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
package org.apache.streampark.spark.core.serializable

import org.apache.avro.generic.GenericContainer
import org.apache.avro.mapred.AvroKey
import org.apache.avro.mapreduce.{AvroKeyOutputFormat, AvroMultipleOutputs}
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapreduce.TaskInputOutputContext

object MultipleAvroOutputsFormat {
  // This seems to be an unfortunate limitation of type inference of lambda defaults within constructor params.
  // If it would work I would just inline this function
  def amoMaker[T](io: TaskInputOutputContext[_, _, AvroKey[T], NullWritable]): MultipleOutputer[AvroKey[T], NullWritable] = new AvroMultipleOutputs(io)
}

class MultipleAvroOutputsFormat[T <: GenericContainer]
  extends MultipleOutputsFormat(
    new AvroKeyOutputFormat[T],
    (io: TaskInputOutputContext[_, _, AvroKey[T], NullWritable]) =>
      MultipleAvroOutputsFormat.amoMaker(io)) {}
