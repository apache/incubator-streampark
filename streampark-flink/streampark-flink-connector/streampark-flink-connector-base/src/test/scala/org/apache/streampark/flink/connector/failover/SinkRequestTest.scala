package org.apache.streampark.flink.connector.failover

import org.junit.jupiter.api.{Assertions, Test}

import scala.collection.JavaConverters._

class SinkRequestTest {
  @Test
  def sqlStatement(): Unit = {
    val statementsList = List(
      "insert into table_1(col1, col2) values(1, 2)",
      "insert into table_1(col1, col2) values(11, 22)",
      "insert into table_1(col1, col2, col3) values(11, 22, 33)",
      "insert into table_2(col1, col2, col3) values(11, 22, 33)",
    )

    val sinkRequest = SinkRequest(statementsList.asJava)

    val expectedSqlStatement = List(
      "insert into table_2(col1, col2, col3) VALUES (11, 22, 33)",
      "insert into table_1(col1, col2) VALUES (1, 2),(11, 22)",
      "insert into table_1(col1, col2, col3) VALUES (11, 22, 33)",
    )

    Assertions.assertTrue(sinkRequest.sqlStatement.toSet == expectedSqlStatement.toSet)

  }
}
