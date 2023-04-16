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

package org.apache.streampark.flink.connector.sink

import org.apache.flink.streaming.api.datastream.DataStreamSink

trait Sink extends Serializable {

  def afterSink[T](
      sink: DataStreamSink[T],
      parallelism: Int,
      name: String,
      uid: String): DataStreamSink[T] = {
    if (parallelism > 0) {
      sink.setParallelism(parallelism)
    }
    if (name != null && name.nonEmpty) {
      sink.name(name)
    }
    if (uid != null && uid.nonEmpty) {
      sink.uid(uid)
    }
    sink
  }

}
