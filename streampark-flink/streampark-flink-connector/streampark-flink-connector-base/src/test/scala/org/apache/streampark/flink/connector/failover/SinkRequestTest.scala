package org.apache.streampark.flink.connector.failover

import org.junit.jupiter.api.{Assertions, Test}

import scala.collection.JavaConverters._

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

    val sinkRequest = SinkRequest(statementsList.asJava)

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
