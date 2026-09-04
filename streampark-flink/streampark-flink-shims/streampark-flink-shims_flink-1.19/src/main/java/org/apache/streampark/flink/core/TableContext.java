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

import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.core.bean.TableContextSpec;

import org.apache.flink.table.api.TableEnvironment;

/** Flink 1.19 table environment context. */
public class TableContext extends AbstractFlinkTable {

    public TableContext(FlinkJobParameters parameter, TableEnvironment tableEnv) {
        super(parameter, tableEnv);
    }

    public TableContext(TableContextSpec contextConfig) {
        this(contextConfig.parameter, contextConfig.tableEnv);
    }

}
