package com.streamxhub.streamx.test.tablesql

import java.util.Scanner


object SQLRegExpTest extends App {

  val sql =
    """
      |CREATE TABLE user_log (
      |    user_id VARCHAR,
      |    item_id VARCHAR,
      |    category_id VARCHAR,
      |    behavior VARCHAR,
      |    ts TIMESTAMP(3)
      |) WITH (
      |'connector.type' = 'kafka', --kafka 123
      |connector.version = universal, --fdsafdsafdsafds
      |connector.topic = user_behavior, --fdsafdsafdsafds
      |connector.properties.bootstrap.servers=test-hadoop-7:9092,test-hadoop-8:9092,test-hadoop-9:9092,
      |connector.startup-mode = earliest-offset,
      |update-mode = append,
      |format.type = json,
      |format.derive-schema = 'true'
      |);
      |""".stripMargin

  val reg = "WITH\\s*\\(\\s*\\n+((.*)\\s*=(.*)(,|)\\s*\\n+)+\\);".r
  val matcher = reg.pattern.matcher(sql)
  if (matcher.find()) {
    val segment = matcher.group()
    val scanner = new Scanner(segment)
    while (scanner.hasNextLine) {
      val line = scanner.nextLine().replaceAll("--(.*)$", "") trim
      val propReg = "\\s*(.*)\\s*=(.*)(,|)\\s*"
      if (line.matches(propReg)) {
        var newLine = line
          .replaceAll("^'|^", "'")
          .replaceAll("('|)\\s*=\\s*('|)", "' = '")
          .replaceAll("('|),\\s*$", "',")
        if (!line.endsWith(",")) {
          newLine = newLine.replaceFirst("('|)\\s*$", "'")
        }
        println(newLine)
      }
    }
  }


}
