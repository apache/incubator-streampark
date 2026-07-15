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

package org.apache.streampark.spark.connector.source;

import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.dstream.DStream;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import scala.Tuple2;

/** Base source class. */
public abstract class Source implements Serializable {

    protected final Logger log =
        StreamParkLoggerFactory.loggerFactory().getLogger(getClass().getName());

    protected final transient StreamingContext ssc;
    protected transient SparkConf sparkConf;
    protected Map<String, String> param;

    protected Source(StreamingContext ssc) {
        this.ssc = ssc;
    }

    public abstract String getPrefix();

    protected SparkConf getSparkConf() {
        if (sparkConf == null) {
            sparkConf = ssc.sparkContext().getConf();
        }
        return sparkConf;
    }

    protected Map<String, String> getParam() {
        if (param == null) {
            param = new HashMap<>();
            scala.collection.Iterator<scala.Tuple2<String, String>> iter =
                scala.collection.JavaConverters.asScalaIteratorConverter(
                    java.util.Arrays.asList(getSparkConf().getAll()).iterator())
                    .asScala()
                    .toIterator();
            String prefix = getPrefix();
            while (iter.hasNext()) {
                Tuple2<String, String> t = iter.next();
                if (t._1().startsWith(prefix) && t._2() != null && !t._2().isEmpty()) {
                    param.put(t._1().substring(prefix.length()), t._2());
                }
            }
        }
        return param;
    }

    public abstract <R> DStream<R> getDStream(Function<Object, R> recordHandler);
}
