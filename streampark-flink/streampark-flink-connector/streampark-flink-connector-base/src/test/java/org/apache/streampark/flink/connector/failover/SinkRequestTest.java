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

package org.apache.streampark.flink.connector.failover;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SinkRequestTest {

    @Test
    void sqlStatement() {
        List<String> statementsList =
                Arrays.asList(
                        "insert into table_1(col1, col2) values(1, 2)",
                        "insert into table_1(col1, col2) values(11, 22)",
                        "insert into table_1(col1, col2, col3) values(11, 22, 33)",
                        "insert into table_2(col1, col2, col3) values(11, 22, 33)");

        SinkRequest sinkRequest = new SinkRequest(statementsList);

        List<String> expectedSqlStatement =
                Arrays.asList(
                        "insert into table_2(col1, col2, col3) VALUES (11, 22, 33)",
                        "insert into table_1(col1, col2) VALUES (1, 2),(11, 22)",
                        "insert into table_1(col1, col2, col3) VALUES (11, 22, 33)");

        assertThat(new HashSet<>(sinkRequest.getSqlStatement()))
                .isEqualTo(new HashSet<>(expectedSqlStatement));
    }
}
