/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.test.flink.java.tablesql;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

public class JavaTableApp {

    public static void main(String[] args) {
        EnvironmentSettings bbSettings = EnvironmentSettings.newInstance().useBlinkPlanner().build();
        TableEnvironment bsTableEnv = TableEnvironment.create(bbSettings);

        String sourceDDL = "CREATE TABLE datagen (  " +
                " f_random INT,  " +
                " f_random_str STRING,  " +
                " ts AS localtimestamp,  " +
                " WATERMARK FOR ts AS ts  " +
                ") WITH (  " +
                " 'connector' = 'datagen',  " +
                " 'rows-per-second'='10',  " +
                " 'fields.f_random.min'='1',  " +
                " 'fields.f_random.max'='5',  " +
                " 'fields.f_random_str.length'='10'  " +
                ")";

        bsTableEnv.executeSql(sourceDDL);

        String sinkDDL = "CREATE TABLE print_table (" +
                " f_random int," +
                " c_val bigint, " +
                " wStart TIMESTAMP(3) " +
                ") WITH ('connector' = 'print') ";

        bsTableEnv.executeSql(sinkDDL);
    }

}




