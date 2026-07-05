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

package org.apache.streampark.flink.connector.mongo.internal;

import org.apache.streampark.common.util.MongoConfig;
import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.mongo.function.MongoQueryFunction;
import org.apache.streampark.flink.connector.mongo.function.MongoResultFunction;
import org.apache.streampark.flink.util.FlinkUtils;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.bson.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/** MongoDB source function with checkpoint state support. */
public class MongoSourceFunction<R> extends RichSourceFunction<R>
        implements CheckpointedFunction, CheckpointListener {

    private static final Logger LOG = LoggerFactory.getLogger(MongoSourceFunction.class);
    private static final String OFFSETS_STATE_NAME = "mongo-source-query-states";

    private volatile boolean running = true;
    private final Properties prop;
    private final String collection;
    private final MongoQueryFunction<R> queryFunc;
    private final MongoResultFunction<R> resultFunc;
    private final RunningFunction runningFunc;
    private final TypeInformation<R> typeInfo;

    private MongoClient client;
    private MongoCollection<Document> mongoCollection;
    private transient ListState<R> state;
    private R last;

    public MongoSourceFunction(
            String collectionName,
            Properties prop,
            MongoQueryFunction<R> queryFunc,
            MongoResultFunction<R> resultFunc,
            RunningFunction runningFunc,
            TypeInformation<R> typeInfo) {
        this.collection = collectionName;
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
    public void open(Configuration parameters) {
        client = MongoConfig.getClient(prop);
        String db = MongoConfig.getProperty(prop, MongoConfig.DATABASE);
        mongoCollection = client.getDatabase(db).getCollection(collection);
    }

    @Override
    public void run(SourceContext<R> ctx) throws Exception {
        while (running) {
            if (runningFunc.running()) {
                synchronized (ctx.getCheckpointLock()) {
                    FindIterable<Document> find = queryFunc.query(last, mongoCollection);
                    if (find != null) {
                        MongoCursor<Document> cursor = find.iterator();
                        List<R> results = new java.util.ArrayList<>();
                        for (R r : resultFunc.result(cursor)) {
                            results.add(r);
                        }
                        for (R x : results) {
                            last = x;
                            ctx.collectWithTimestamp(last, System.currentTimeMillis());
                        }
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
    public void close() {
        if (client != null) {
            client.close();
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
            LOG.error("MongoSource snapshotState called on closed source");
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        LOG.info("MongoSource snapshotState initialize");
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
        LOG.info("MongoSource checkpointComplete: {}", checkpointId);
    }
}
