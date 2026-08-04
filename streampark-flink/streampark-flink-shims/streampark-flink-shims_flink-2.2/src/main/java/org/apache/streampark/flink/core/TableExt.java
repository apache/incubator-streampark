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

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/** Table API extensions for Flink 2.2 Java stream-table applications. */
public final class TableExt {

    private TableExt() {
    }

    /** Table alias helper (Scala {@code ->} operator equivalent: {@code as}). */
    public static final class Table {

        private final org.apache.flink.table.api.Table flinkTable;

        public Table(org.apache.flink.table.api.Table table) {
            this.flinkTable = table;
        }

        public org.apache.flink.table.api.Table as(String field, String... fields) {
            return flinkTable.as(field, fields);
        }
    }

    /** Table-to-DataStream conversion helpers. */
    public static class TableConversions {

        private final org.apache.flink.table.api.Table flinkTable;

        private final StreamTableEnvironment streamTableEnv;

        public TableConversions(
                                org.apache.flink.table.api.Table table, StreamTableEnvironment streamTableEnv) {
            this.flinkTable = table;
            this.streamTableEnv = streamTableEnv;
        }

        /** Changelog stream conversion (Scala {@code \\} operator equivalent). */
        public DataStream<Row> toChangelogDataStream() {
            return streamTableEnv.toDataStream(flinkTable);
        }
    }
}
