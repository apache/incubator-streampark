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

package org.apache.streampark.spark.core;

import org.apache.streampark.common.conf.ConfigKeys;

import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.StreamingContext;

import scala.runtime.AbstractFunction0;

/** Base class for Spark streaming applications. */
public abstract class SparkStreaming extends Spark {

    protected transient StreamingContext context;

    protected StreamingContext getContext() {
        if (context == null) {
            if (checkpoint == null || checkpoint.isEmpty()) {
                context = createContext();
            } else {
                context =
                        StreamingContext.getOrCreate(
                                checkpoint,
                                new AbstractFunction0<StreamingContext>() {
                                    @Override
                                    public StreamingContext apply() {
                                        return createContext();
                                    }
                                },
                                sparkSession.sparkContext().hadoopConfiguration(),
                                createOnError);
                context.checkpoint(checkpoint);
            }
        }
        return context;
    }

    private StreamingContext createContext() {
        int duration = Integer.parseInt(sparkConf.get(ConfigKeys.KEY_SPARK_BATCH_DURATION(), "5"));
        return new StreamingContext(sparkSession.sparkContext(), new Duration(duration * 1000L));
    }

    @Override
    protected final void start() {
        StreamingContext ctx = getContext();
        ctx.start();
        ctx.awaitTermination();
    }

    @Override
    protected void config(SparkConf sparkConf) {}

    @Override
    protected void ready() {}

    @Override
    protected void destroy() {}
}
