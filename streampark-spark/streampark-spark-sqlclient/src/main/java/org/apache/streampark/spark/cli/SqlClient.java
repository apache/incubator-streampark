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

import org.apache.streampark.spark.core.util.SparkSqlExecutor;

/** Spark SQL client entry point. */
public final class SqlClient {

    private static final String MODE = "BATCH";

    private SqlClient() {
    }

    public static void main(String[] args) {
        switch (MODE) {
            case "STREAMING":
            case "AUTOMATIC":
                SparkSqlExecutor.runStreaming(args);
                break;
            case "BATCH":
                SparkSqlExecutor.runBatch(args);
                break;
            default:
                throw new IllegalArgumentException(
                    "Usage: runtime execution-mode invalid, optional [STREAMING|BATCH|AUTOMATIC]");
        }
    }
}
