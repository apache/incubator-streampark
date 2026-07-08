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

package org.apache.streampark.flink.connector.clickhouse.internal;

import org.apache.streampark.common.util.JdbcUtils;
import org.apache.streampark.flink.connector.clickhouse.conf.ClickHouseJdbcConfig;
import org.apache.streampark.flink.connector.clickhouse.util.ClickhouseConvertUtils;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import java.lang.reflect.Field;
import java.sql.Connection; import java.sql.Statement;
import java.util.ArrayList; import java.util.List; import java.util.Map;
import java.util.Properties; import java.util.concurrent.atomic.AtomicLong;

public class ClickHouseSinkFunction<T> extends RichSinkFunction<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseSinkFunction.class);
    private Connection connection; private Statement statement;
    private final ClickHouseJdbcConfig clickHouseConf;
    private final int batchSize; private final AtomicLong offset = new AtomicLong(0L);
    private long timestamp = 0L; private final long flushInterval;
    private final List<String> sqlValues = new ArrayList<>();
    private String insertSqlPrefixes;
    private final TransformFunction<T, String> sqlFunc;

    public ClickHouseSinkFunction(Properties properties, TransformFunction<T, String> sqlFunc) {
        this.clickHouseConf = new ClickHouseJdbcConfig(properties);
        this.batchSize = clickHouseConf.batchSize;
        this.flushInterval = clickHouseConf.flushInterval;
        this.sqlFunc = sqlFunc;
    }

    @Override public void open(Configuration parameters) throws Exception {
        String user = clickHouseConf.user; String driver = clickHouseConf.driverClassName;
        ClickHouseProperties chProps = new ClickHouseProperties();
        if (user != null && driver != null) { Class.forName(driver); chProps.setUser(user); }
        else if (driver != null) Class.forName(driver);
        else if (user != null) chProps.setUser(user);
        for (Map.Entry<Object,Object> x : clickHouseConf.sinkOption.getInternalConfig().entrySet()) {
            try {
                Field field = chProps.getClass().getDeclaredField(x.getKey().toString());
                field.setAccessible(true);
                String tn = field.getType().getSimpleName();
                if ("String".equals(tn)) field.set(chProps, x.getValue());
                else if ("int".equals(tn) || "Integer".equals(tn)) field.set(chProps, Integer.parseInt(x.getValue().toString()));
                else if ("long".equals(tn) || "Long".equals(tn)) field.set(chProps, Long.parseLong(x.getValue().toString()));
                else if ("boolean".equals(tn) || "Boolean".equals(tn)) field.set(chProps, Boolean.parseBoolean(x.getValue().toString()));
            } catch (NoSuchFieldException e) {
                LOG.warn("ClickHouseProperties config error, property:{} invalid", x.getKey());
            }
        }
        connection = new ClickHouseDataSource(clickHouseConf.jdbcUrl, chProps).getConnection();
    }

    @Override public void invoke(T value, SinkFunction.Context context) throws Exception {
        String sql = sqlFunc != null ? sqlFunc.transform(value) : ClickhouseConvertUtils.convert(value);
        if (batchSize == 1) {
            connection.prepareStatement(sql).executeUpdate();
        } else {
            sqlValues.add(sql);
            long cnt = offset.incrementAndGet();
            long now = System.currentTimeMillis();
            if (cnt % batchSize == 0 || now - timestamp > flushInterval) execBatch();
        }
    }

    @Override public void close() throws Exception { execBatch(); JdbcUtils.close(statement, connection); }

    private void execBatch() throws Exception {
        if (offset.get() > 0) {
            try {
                LOG.info("ClickHouseSink batch {} insert begin..", offset.get());
                offset.set(0);
                String valuesStr = String.join(",", sqlValues);
                // Values are generated by the connector transform, not external user SQL input.
                @SuppressWarnings("java:S2077")
                String batchSql = insertSqlPrefixes + " " + valuesStr;
                connection.prepareStatement(batchSql).executeUpdate();
                timestamp = System.currentTimeMillis();
            } finally { sqlValues.clear(); }
        }
    }
}
