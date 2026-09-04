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

import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;

/** Flink 2.0 stream-table environment context. */
public class StreamTableContext extends AbstractFlinkStreamTable {

    public StreamTableContext(StreamTableContextSpec streamContextConfig) {
        super(streamContextConfig.parameter, streamContextConfig.streamEnv, streamContextConfig.streamTableEnv);
    }

    @Override
    public void createTemporaryTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        getStreamTableEnv().createTemporaryTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public boolean createTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        return getStreamTableEnv().createTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createView(String path, Table view) {
        getStreamTableEnv().createView(path, view);
    }

    @Override
    public boolean createView(String path, Table view, boolean ignoreIfExists) {
        return getStreamTableEnv().createView(path, view, ignoreIfExists);
    }

    @Override
    public boolean dropTable(String path) {
        return getStreamTableEnv().dropTable(path);
    }

    @Override
    public boolean dropTable(String path, boolean ignoreIfNotExists) {
        return getStreamTableEnv().dropTable(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropView(String path) {
        return getStreamTableEnv().dropView(path);
    }

    @Override
    public boolean dropView(String path, boolean ignoreIfNotExists) {
        return getStreamTableEnv().dropView(path, ignoreIfNotExists);
    }

    @Override
    public StreamStatementSet createStatementSet() {
        return getStreamTableEnv().createStatementSet();
    }

}
