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

package org.apache.streampark.flink.connector.clickhouse.conf;
import java.io.Serializable; import java.util.Properties;
public class ClickHouseJdbcConfig implements Serializable {
    public final ClickHouseSinkConfigOption sinkOption;
    public final String user, password, jdbcUrl, driverClassName;
    public final int batchSize; public final long flushInterval;
    public ClickHouseJdbcConfig(Properties parameters) {
        sinkOption = ClickHouseSinkConfigOption.of(parameters);
        user = sinkOption.user.get(); password = sinkOption.password.get();
        jdbcUrl = sinkOption.jdbcUrl.get(); driverClassName = sinkOption.driverClassName.get();
        batchSize = sinkOption.batchSize.get(); flushInterval = sinkOption.flushInterval.get();
    }
}
