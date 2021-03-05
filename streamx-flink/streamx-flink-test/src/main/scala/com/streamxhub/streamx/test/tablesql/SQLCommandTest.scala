package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.common.util.SQLCommandUtil

object SQLCommandTest extends App {

  val sql =
    """
      |CREATE TABLE xuser_log (
      |    user_id VARCHAR,
      |    item_id VARCHAR,
      |    category_id VARCHAR,
      |    behavior VARCHAR,
      |    ts TIMESTAMP(3)
      |) WITH (
      |'connector.type' = 'kafka', -- 使用 kafka connector
      |'connector.version' = 'universal',  -- kafka 版本，universal 支持 0.11 以上的版本
      |'connector.topic' = 'user_behavior',  -- kafka topic
      |'connector.properties.bootstrap.servers'='test-hadoop-7:9092,test-hadoop-8:9092,test-hadoop-9:9092',
      |'connector.startup-mode' = 'earliest-offset', -- 从起始 offset 开始读取
      |'update-mode' = 'append',
      |'format.type' = 'json',  -- 数据源格式为 json
      |'format.derive-schema' = 'true' -- 从 DDL schema 确定 json 解析规则
      |);
      |
      |CREATE TABLE pvuv_sink (
      |    dt VARCHAR,
      |    pv BIGINT,
      |    uv BIGINT
      |) WITH (
      |'connector.type' = 'jdbc', -- 使用 jdbc connector
      |'connector.url' = 'jdbc:mysql://10.2.39.80:3306/test', -- jdbc url
      |'connector.table' = 'pvuv_sink', -- 表名
      |'connector.username' = 'hopsonone', -- 用户名
      |'connector.password' = 'hopsonone123', -- 密码
      |'connector.write.flush.max-rows' = '1' -- 默认5000条，为了演示改为1条
      |);
      |
      |INSERT INTO pvuv_sink
      |SELECT
      |  DATE_FORMAT(ts, 'yyyy-MM-dd HH:00') dt,
      |  COUNT(*) AS pv,
      |  COUNT(DISTINCT user_id) AS uv
      |FROM user_log
      |GROUP BY DATE_FORMAT(ts, 'yyyy-MM-dd HH:00');
      |
      |""".stripMargin

  val sqlError = SQLCommandUtil.verifySQL(sql)
  println(sqlError.sql)
}
