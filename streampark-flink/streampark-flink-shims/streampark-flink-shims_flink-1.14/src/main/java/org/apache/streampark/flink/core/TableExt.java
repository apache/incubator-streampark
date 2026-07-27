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
import org.apache.flink.streaming.api.scala.DataStream;
import org.apache.flink.table.api.Table;

/**
 * Table extension utilities for Flink Table API.
 */
public final class TableExt {

    private TableExt() {
    }

    public static TableWrapper wrap(Table table) {
        return new TableWrapper(table);
    }

    public static TableConversions conversions(Table table) {
        return new TableConversions(table);
    }

    public static final class TableWrapper {

        private final Table table;

        public TableWrapper(Table table) {
            this.table = table;
        }

        public Table alias(String field, String... fields) {
            return table.as(field, fields);
        }

        public Table $minus$greater(String field, String... fields) {
            return alias(field, fields);
        }
    }

    public static final class TableConversions extends org.apache.flink.table.api.bridge.scala.TableConversions {

        public DataStream<org.apache.flink.types.Row> toDataStreamRow() {
            return toDataStream();
        }

        public <T> DataStream<T> appendStream(StreamTableContext context, TypeInformation<T> typeInfo) {
            context.isConvertedToDataStream = true;
            return super.toAppendStream(typeInfo);
        }

        public <T> DataStream<scala.Tuple2<Object, T>> retractStream(
                                                                     StreamTableContext context,
                                                                     TypeInformation<T> typeInfo) {
            context.isConvertedToDataStream = true;
            return super.toRetractStream(typeInfo);
        }

        public <T> DataStream<T> toAppendStream(StreamTableContext context, TypeInformation<T> typeInfo) {
            context.isConvertedToDataStream = true;
            return super.toAppendStream(typeInfo);
        }

        public <T> DataStream<scala.Tuple2<Object, T>> toRetractStream(
                                                                       StreamTableContext context,
                                                                       TypeInformation<T> typeInfo) {
            context.isConvertedToDataStream = true;
            return super.toRetractStream(typeInfo);
        }

        public TableConversions(Table table) {
            super(table);
        }
    }
}
