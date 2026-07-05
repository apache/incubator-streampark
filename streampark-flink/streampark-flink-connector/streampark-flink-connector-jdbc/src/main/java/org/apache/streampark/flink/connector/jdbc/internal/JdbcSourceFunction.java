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

package org.apache.streampark.flink.connector.jdbc.internal;

import org.apache.streampark.common.util.JdbcUtils;
import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.function.SQLQueryFunction;
import org.apache.streampark.flink.connector.function.SQLResultFunction;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** JDBC source function with checkpoint state support. */
public class JdbcSourceFunction<R> extends RichSourceFunction<R>
        implements CheckpointedFunction, CheckpointListener {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSourceFunction.class);
    private static final String OFFSETS_STATE_NAME = "jdbc-source-query-states";

    private volatile boolean running = true;
    private final Properties jdbc;
    private final SQLQueryFunction<R> sqlFunc;
    private final SQLResultFunction<R> resultFunc;
    private final RunningFunction runningFunc;
    private final TypeInformation<R> typeInfo;

    private transient ListState<R> state;
    private R last;

    public JdbcSourceFunction(
            Properties jdbc,
            SQLQueryFunction<R> sqlFunc,
            SQLResultFunction<R> resultFunc,
            RunningFunction runningFunc,
            TypeInformation<R> typeInfo) {
        this.jdbc = jdbc;
        this.sqlFunc = sqlFunc;
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
    public void run(SourceContext<R> ctx) throws Exception {
        while (running) {
            if (runningFunc.running()) {
                synchronized (ctx.getCheckpointLock()) {
                    String sql = sqlFunc.query(last);
                    List<Map<String, Object>> result = JdbcUtils.select(sql, jdbc);
                    Iterable<Map<String, ?>> rows = (Iterable) result;
                    Iterable<R> records = resultFunc.result(rows);
                    for (R x : records) {
                        last = x;
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
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        if (running) {
            state.clear();
            if (last != null) {
                state.add(last);
            }
        } else {
            LOG.error("JdbcSource snapshotState called on closed source");
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        LOG.info("JdbcSource snapshotState initialize");
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
        LOG.info("JdbcSource checkpointComplete: {}", checkpointId);
    }
}
