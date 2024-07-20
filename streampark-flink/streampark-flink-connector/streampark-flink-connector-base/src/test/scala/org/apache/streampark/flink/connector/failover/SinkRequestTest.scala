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

package org.apache.streampark.flink.connector.failover

import org.junit.jupiter.api.{Assertions, Test}

import org.apache.streampark.common.util.Implicits._

class SinkRequestTest {
  @Test
  def sqlStatement(): Unit = {
    // input statements
    val statementsList = List(
      "insert into table_1(col1, col2) values(1, 2)",
      "insert into table_1(col1, col2) values(11, 22)",
      "insert into table_1(col1, col2, col3) values(11, 22, 33)",
      "insert into table_2(col1, col2, col3) values(11, 22, 33)",
    )

    val sinkRequest = SinkRequest(statementsList)

    // expected result
    val expectedSqlStatement = List(
      "insert into table_2(col1, col2, col3) VALUES (11, 22, 33)",
      "insert into table_1(col1, col2) VALUES (1, 2),(11, 22)",
      "insert into table_1(col1, col2, col3) VALUES (11, 22, 33)",
    )

    // comparison of result should be based on Set, that is, there is no need to care about the order of elements
    Assertions.assertTrue(sinkRequest.sqlStatement.toSet == expectedSqlStatement.toSet)

  }
}
