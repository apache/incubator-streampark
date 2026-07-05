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

package org.apache.streampark.flink.connector.hbase.internal;

import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.hbase.bean.HBaseQuery;
import org.apache.streampark.flink.connector.hbase.function.HBaseQueryFunction;
import org.apache.streampark.flink.connector.hbase.function.HBaseResultFunction;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Properties;

/** HBase source function with checkpoint state support. */
public class HBaseSourceFunction<R> extends RichSourceFunction<R>
        implements CheckpointedFunction, CheckpointListener {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseSourceFunction.class);
    private static final String OFFSETS_STATE_NAME = "hbase-source-query-states";

    private volatile boolean running = true;
    private final Properties prop;
    private final HBaseQueryFunction<R> queryFunc;
    private final HBaseResultFunction<R> resultFunc;
    private final RunningFunction runningFunc;
    private final TypeInformation<R> typeInfo;

    private transient Table table;
    private HBaseQuery query;
    private transient ListState<R> state;
    private R last;

    public HBaseSourceFunction(
            Properties prop,
            HBaseQueryFunction<R> queryFunc,
            HBaseResultFunction<R> resultFunc,
            RunningFunction runningFunc,
            TypeInformation<R> typeInfo) {
        this.prop = prop;
        this.queryFunc = queryFunc;
        this.resultFunc = resultFunc;
        this.runningFunc =
                runningFunc != null
                        ? runningFunc
                        : new RunningFunction() {
                            @Override
                            public Boolean running() {
                                return true;
                            }
                        };
        this.typeInfo = typeInfo;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
    }

    @Override
    public void run(SourceContext<R> ctx) throws Exception {
        while (running) {
            if (runningFunc.running()) {
                synchronized (ctx.getCheckpointLock()) {
                    query = queryFunc.query(last);
                    if (query == null || query.getTable() == null) {
                        throw new IllegalArgumentException(
                                "[StreamPark] HBaseSource query and query's param table must not be null ");
                    }
                    table = query.getTable(prop);
                    ResultScanner scanner = table.getScanner(query);
                    for (Result result : scanner) {
                        last = resultFunc.result(result);
                        ctx.collectWithTimestamp(last, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    @Override
    public void cancel() {
        running = false;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (table != null) {
            table.close();
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        if (running) {
            state.clear();
            if (last != null) {
                state.add(last);
            }
        } else {
            LOG.error("HBaseSource snapshotState called on closed source");
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        LOG.info("HBaseSource snapshotState initialize");
        @SuppressWarnings("unchecked")
        TypeInformation<R> stateType =
                typeInfo != null ? typeInfo : (TypeInformation<R>) TypeInformation.of(Object.class);
        state = FlinkUtils.getUnionListState(context, OFFSETS_STATE_NAME, stateType);
        Iterator<R> it = state.get().iterator();
        if (it.hasNext()) {
            last = it.next();
        }
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {
        LOG.info("HBaseSource checkpointComplete: {}", checkpointId);
    }
}
