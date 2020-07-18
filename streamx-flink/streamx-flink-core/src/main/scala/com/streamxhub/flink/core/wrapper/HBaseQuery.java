/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.core.wrapper;

import com.streamxhub.common.util.HBaseClient;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.Filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

public class HBaseQuery extends Scan implements Serializable {

    private String table;
    private volatile Table htable;

    public HBaseQuery(String table) {
        this.table = table;
    }

    public HBaseQuery(String table, byte[] startRow, Filter filter) {
        super(startRow, filter);
        this.table = table;
    }

    public HBaseQuery(String table, byte[] startRow) {
        super(startRow);
        this.table = table;
    }

    public HBaseQuery(String table, byte[] startRow, byte[] stopRow) {
        super(startRow, stopRow);
        this.table = table;
    }

    public HBaseQuery(String table, Scan scan) throws IOException {
        super(scan);
        this.table = table;
    }

    public HBaseQuery(String table, Get get) {
        super(get);
        this.table = table;
    }

    public String getTable() {
        return table;
    }

    public Table getTable(Properties prop) {
        if (htable == null) {
            synchronized (HBaseQuery.class) {
                if (htable == null) {
                    htable = HBaseClient.apply(prop).table(this.getTable());
                }
            }
        }
        return htable;
    }
}
