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

package org.apache.streampark.flink.connector.hbase.bean;

import org.apache.streampark.common.util.HBaseClient;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/**
 * Wrapping a HBase query condition object, wrapping the scan and get two query methods.
 */
public class HBaseQuery extends Scan implements Serializable {

    private final String table;
    private volatile Table htable;
    private transient Scan scan;
    private transient Get get;

    public HBaseQuery(String table, Scan scan) throws IOException {
        super(scan);
        this.table = table;
        this.scan = scan;
    }

    public HBaseQuery(String table, Get get) {
        super(get);
        this.table = table;
        this.get = get;
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

    public String getTable() {
        return table;
    }

    public Scan getScan() {
        return scan;
    }

    public Get getGet() {
        return get;
    }
}
