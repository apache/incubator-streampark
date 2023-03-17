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

package org.apache.streampark.flink.connector.mongo.source;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.mongo.function.MongoQueryFunction;
import org.apache.streampark.flink.connector.mongo.function.MongoResultFunction;
import org.apache.streampark.flink.connector.mongo.internal.MongoSourceFunction;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

public class MongoJavaSource<T> {
  private final StreamingContext context;
  private final Properties property;

  public MongoJavaSource(StreamingContext context, Properties property) {
    this.context = context;
    this.property = property;
  }

  public DataStreamSource<T> getDataStream(
      String collectionName,
      MongoQueryFunction<T> queryFunction,
      MongoResultFunction<T> resultFunction,
      RunningFunction runningFunc) {

    Utils.notNull(collectionName, "collectionName must not be null");
    Utils.notNull(queryFunction, "queryFunction must not be null");
    Utils.notNull(resultFunction, "resultFunction must not be null");
    MongoSourceFunction<T> sourceFunction =
        new MongoSourceFunction<>(
            collectionName, property, queryFunction, resultFunction, runningFunc, null);
    return context.getJavaEnv().addSource(sourceFunction);
  }
}
