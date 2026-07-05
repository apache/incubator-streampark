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

package org.apache.streampark.spark.core.util;

import org.apache.spark.SparkContext;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

/** SQLContext singleton. */
public final class SQLContextUtil {

    private static SQLContext instance;
    private static SQLContext hiveContext;

    private SQLContextUtil() {}

    public static SQLContext getSqlContext(SparkContext sparkContext) {
        if (instance == null) {
            instance =
                    SparkSession.builder()
                            .config(sparkContext.getConf())
                            .getOrCreate()
                            .sqlContext();
        }
        return instance;
    }

    public static SQLContext getHiveContext(SparkContext sparkContext) {
        if (hiveContext == null) {
            hiveContext =
                    SparkSession.builder()
                            .config(sparkContext.getConf())
                            .enableHiveSupport()
                            .getOrCreate()
                            .sqlContext();
        }
        return hiveContext;
    }
}
