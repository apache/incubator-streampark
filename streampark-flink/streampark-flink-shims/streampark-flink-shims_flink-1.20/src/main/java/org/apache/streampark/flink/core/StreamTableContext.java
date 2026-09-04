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

package org.apache.streampark.flink.core;

import org.apache.streampark.flink.core.bean.StreamTableContextSpec;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.TableAggregateFunction;
import org.apache.flink.table.functions.TableFunction;

/**
 * Flink 1.20 stream-table environment context.
 *
 * <p>Legacy registration methods remain in this version-specific adapter because Flink 2.x removed
 * them from {@code StreamTableEnvironment}. Keeping them outside the shared base prevents 1.x API
 * references from leaking into Flink 2.x shims.
 */
public class StreamTableContext extends AbstractFlinkStreamTable {

    public StreamTableContext(StreamTableContextSpec streamContextConfig) {
        super(streamContextConfig.parameter, streamContextConfig.streamEnv, streamContextConfig.streamTableEnv);
    }

    @Override
    public <T> void registerFunction(String name, TableFunction<T> tableFunction) {
        getStreamTableEnv().registerFunction(name, tableFunction);
    }

    @Override
    public <T, ACC> void registerFunction(String name, AggregateFunction<T, ACC> aggregateFunction) {
        getStreamTableEnv().registerFunction(name, aggregateFunction);
    }

    @Override
    public <T, ACC> void registerFunction(
                                          String name,
                                          TableAggregateFunction<T, ACC> tableAggregateFunction) {
        getStreamTableEnv().registerFunction(name, tableAggregateFunction);
    }

    @Override
    public <T> void registerDataStream(String name, DataStream<T> dataStream) {
        getStreamTableEnv().registerDataStream(name, dataStream);
    }

    @Override
    public StreamStatementSet createStatementSet() {
        return getStreamTableEnv().createStatementSet();
    }

}
