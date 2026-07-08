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

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Time;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Print sink for debugging. */
public class ShowSink<T> extends Sink<T> {

    private final Map<String, String> initParams;
    private Properties prop;

    public ShowSink(SparkContext sc) {
        this(sc, Collections.emptyMap());
    }

    public ShowSink(SparkContext sc, Map<String, String> initParams) {
        super(sc);
        this.initParams = initParams;
    }

    @Override
    public String getPrefix() {
        return "spark.sink.show.";
    }

    private Properties getProp() {
        if (prop == null) {
            prop = filterProp(getParam(), initParams, getPrefix(), "");
        }
        return prop;
    }

    @Override
    public void sink(JavaRDD<T> rdd, Time time) {
        int num = Integer.parseInt(getProp().getProperty("num", "10"));
        List<T> firstNum = rdd.take(num + 1);
        System.out.println("-------------------------------------------");
        System.out.println("Time: " + time);
        System.out.println("-------------------------------------------");
        for (int i = 0; i < Math.min(num, firstNum.size()); i++) {
            System.out.println(firstNum.get(i));
        }
        if (firstNum.size() > num) {
            System.out.println("...");
        }
        System.out.println();
    }
}
