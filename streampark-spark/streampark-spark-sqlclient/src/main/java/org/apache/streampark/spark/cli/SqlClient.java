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

package org.apache.streampark.spark.cli;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.spark.core.SparkBatch;
import org.apache.streampark.spark.core.SparkStreaming;
import org.apache.streampark.spark.core.util.ParameterTool;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/** Spark SQL client entry. */
public class SqlClient {

    public static void main(String[] args) {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);
        String sql = parameterTool.get(ConfigKeys.KEY_SPARK_SQL);
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("Usage: spark sql cannot be null");
        }
        try {
            DeflaterUtils.unzipString(sql);
        } catch (Exception e) {
            throw new IllegalArgumentException("Usage: spark sql is invalid or null, please check");
        }

        String mode = "BATCH";
        switch (mode) {
            case "STREAMING":
            case "AUTOMATIC":
                new StreamSqlApp().main(args);
                break;
            case "BATCH":
                new BatchSqlApp().main(args);
                break;
            default:
                throw new IllegalArgumentException(
                    "Usage: runtime execution-mode invalid, optional [STREAMING|BATCH|AUTOMATIC]");
        }
    }

    private static final class BatchSqlApp extends SparkBatch {

        @Override
        protected Dataset<Row> handle(String sql) {
            return super.handle(sql);
        }
    }

    private static final class StreamSqlApp extends SparkStreaming {

        @Override
        protected Dataset<Row> handle(String sql) {
            return super.handle(sql);
        }
    }
}
