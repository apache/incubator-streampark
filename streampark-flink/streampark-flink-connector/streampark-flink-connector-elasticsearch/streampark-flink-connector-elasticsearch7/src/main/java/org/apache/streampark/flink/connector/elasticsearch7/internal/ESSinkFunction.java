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

package org.apache.streampark.flink.connector.elasticsearch7.internal;

import org.apache.streampark.flink.connector.function.TransformFunction;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESSinkFunction<T> implements ElasticsearchSinkFunction<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ESSinkFunction.class);
    private final TransformFunction<T, ActionRequest> func;

    public ESSinkFunction(TransformFunction<T, ActionRequest> func) {
        this.func = func;
    }

    @Override
    public void process(T element, RuntimeContext ctx, RequestIndexer indexer) {
        ActionRequest request = func.transform(element);
        if (request instanceof IndexRequest) {
            indexer.add((IndexRequest) request);
        } else if (request instanceof DeleteRequest) {
            indexer.add((DeleteRequest) request);
        } else if (request instanceof UpdateRequest) {
            indexer.add((UpdateRequest) request);
        } else {
            LOG.error("ElasticsearchSinkFunction add ActionRequest is deprecated, please use IndexRequest|DeleteRequest|UpdateRequest ");
            indexer.add(request);
        }
    }
}
