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

package org.apache.streampark.spark.connector.sink;

import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.dstream.DStream;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

/** Base sink class. */
public abstract class Sink<T> implements Serializable {

    protected final Logger log =
            StreamParkLoggerFactory.loggerFactory().getLogger(getClass().getName());

    protected final transient SparkContext sc;
    protected transient org.apache.spark.SparkConf sparkConf;
    protected Map<String, String> param;

    protected Sink(SparkContext sc) {
        this.sc = sc;
    }

    public abstract String getPrefix();

    protected org.apache.spark.SparkConf getSparkConf() {
        if (sparkConf == null) {
            sparkConf = sc.getConf();
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
                scala.Tuple2<String, String> t = iter.next();
                if (t._1().startsWith(prefix) && t._2() != null && !t._2().isEmpty()) {
                    param.put(t._1().substring(prefix.length()), t._2());
                }
            }
        }
        return param;
    }

    protected Properties filterProp(
            Map<String, String> param, Map<String, String> overrides, String prefix, String replacement) {
        Properties p = new Properties();
        Map<String, String> map = new HashMap<>(param);
        map.putAll(overrides);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (prefix.isEmpty() || entry.getKey().startsWith(prefix)) {
                p.put(entry.getKey().replace(prefix, replacement), entry.getValue());
            }
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    public void sink(DStream<T> dStream) {
        ClassTag<T> tag = (ClassTag<T>) ClassTag$.MODULE$.Any();
        JavaDStream.fromDStream(dStream, tag).foreachRDD((rdd, time) -> sink(rdd, time));
    }

    public abstract void sink(JavaRDD<T> rdd, Time time);
}
