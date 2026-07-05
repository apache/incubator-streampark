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

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.bridge.java.BatchTableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/** Table API extensions for Flink 1.13 Java stream-table applications. */
public final class TableExt {

    private TableExt() {}

    /** Table alias helper (Scala {@code ->} operator equivalent: {@code as}). */
    public static final class Table {

        private final org.apache.flink.table.api.Table table;

        public Table(org.apache.flink.table.api.Table table) {
            this.table = table;
        }

        public org.apache.flink.table.api.Table as(String field, String... fields) {
            return table.as(field, fields);
        }
    }

    /** Table-to-DataStream/DataSet conversion helpers. */
    public static class TableConversions {

        private final org.apache.flink.table.api.Table table;

        public TableConversions(org.apache.flink.table.api.Table table) {
            this.table = table;
        }

        /** Batch DataSet conversion (Scala {@code \\} operator equivalent). */
        public <T> DataSet<T> toDataSet(
                TypeInformation<T> typeInfo, StreamTableContext context) {
            context.isConvertedToDataStream = true;
            StreamTableEnvironment tableEnv = context.getStreamTableEnv();
            if (tableEnv instanceof BatchTableEnvironment) {
                return ((BatchTableEnvironment) tableEnv).toDataSet(table, typeInfo);
            }
            throw new TableException("toDataSet is only supported in batch TableEnvironment");
        }

        /** Append stream conversion (Scala {@code >>} operator equivalent). */
        public <T> DataStream<T> toAppendDataStream(
                TypeInformation<T> typeInfo, StreamTableContext context) {
            context.isConvertedToDataStream = true;
            return context.toAppendStream(table, typeInfo);
        }

        /** Retract stream conversion (Scala {@code <<} operator equivalent). */
        public <T> DataStream<Tuple2<Boolean, T>> toRetractDataStream(
                TypeInformation<T> typeInfo, StreamTableContext context) {
            context.isConvertedToDataStream = true;
            return context.toRetractStream(table, typeInfo);
        }
    }
}
