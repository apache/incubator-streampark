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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.util.DeflaterUtils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlinkSqlTest {

    private static final String SQL = "SELECT * FROM source;";

    @Test
    void shouldApplyPersistedSqlToApplication() {
        FlinkSql flinkSql = flinkSql(DeflaterUtils.zipString(SQL));
        FlinkApplication application = new FlinkApplication();

        flinkSql.decode();
        flinkSql.applyToApplication(application);

        assertThat(decodeTransportValue(application.getFlinkSql())).isEqualTo(SQL);
        assertThat(application.getSqlId()).isEqualTo(flinkSql.getId());
    }

    @Test
    void shouldApplyPlainSqlToApplication() {
        FlinkApplication application = new FlinkApplication();

        flinkSql(SQL).applyToApplication(application);

        assertThat(decodeTransportValue(application.getFlinkSql())).isEqualTo(SQL);
    }

    @Test
    void shouldEncodePersistedSqlForHistoryApi() {
        FlinkSql flinkSql = flinkSql(DeflaterUtils.zipString(SQL));

        flinkSql.base64Encode();

        assertThat(decodeTransportValue(flinkSql.getSql())).isEqualTo(SQL);
    }

    @Test
    void shouldRejectCorruptPersistedSql() {
        FlinkSql flinkSql = flinkSql("not-compressed-sql");

        assertThatThrownBy(flinkSql::decode)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to decompress Flink SQL id=100002");
    }

    private static FlinkSql flinkSql(String sql) {
        FlinkSql flinkSql = new FlinkSql();
        flinkSql.setId(100002L);
        flinkSql.setSql(sql);
        return flinkSql;
    }

    private static String decodeTransportValue(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
