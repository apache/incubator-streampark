---
title: '快速开始'
sidebar: true
author: 'benjobs'
date: 2020/04/13
original: true
---



## 如何使用

streamx-console定位是流批一体的大数据平台,一站式解决方案,使用起来非常简单,没有复杂的概念和繁琐的操作,标准的Flink程序(安装Flink官方要去的结构和规范)和用`streamx`开发的项目都做了很好的支持,下面我们使用`streamx-quickstart`来快速开启streamx-console之旅

`streamx-quickstart`是StreamX 开发Flink的上手示例程序,具体请查阅

* Github: [https://github.com/streamxhub/streamx-quickstart.git](https://github.com/streamxhub/streamx-quickstart.git)
* Gitee: [https://gitee.com/benjobs/streamx-quickstart.git](https://gitee.com/benjobs/streamx-quickstart.git)

### 部署DataStream任务

下面的示例演示了如何部署一个DataStream应用

<video src="http://assets.streamxhub.com/20210408008.mp4" controls="controls" width="100%" height="100%"></video>

### 部署FlinkSql任务

下面的示例演示了如何部署一个FlinkSql应用

<video src="http://assets.streamxhub.com/flinksql.mp4" controls="controls" width="100%" height="100%"></video>

* 项目演示使用到的flink sql如下

```sql 
CREATE TABLE user_log (
    user_id VARCHAR,
    item_id VARCHAR,
    category_id VARCHAR,
    behavior VARCHAR,
    ts TIMESTAMP(3)
) WITH (
'connector.type' = 'kafka', -- 使用 kafka connector
'connector.version' = 'universal',  -- kafka 版本，universal 支持 0.11 以上的版本
'connector.topic' = 'user_behavior',  -- kafka topic
'connector.properties.bootstrap.servers'='kafka-1:9092,kafka-2:9092,kafka-3:9092',
'connector.startup-mode' = 'earliest-offset', -- 从起始 offset 开始读取
'update-mode' = 'append',
'format.type' = 'json',  -- 数据源格式为 json
'format.derive-schema' = 'true' -- 从 DDL schema 确定 json 解析规则
);

CREATE TABLE pvuv_sink (
    dt VARCHAR,
    pv BIGINT,
    uv BIGINT
) WITH (
'connector.type' = 'jdbc', -- 使用 jdbc connector
'connector.url' = 'jdbc:mysql://test-mysql:3306/test', -- jdbc url
'connector.table' = 'pvuv_sink', -- 表名
'connector.username' = 'root', -- 用户名
'connector.password' = '123456', -- 密码
'connector.write.flush.max-rows' = '1' -- 默认5000条，为了演示改为1条
);

INSERT INTO pvuv_sink
SELECT
  DATE_FORMAT(ts, 'yyyy-MM-dd HH:00') dt,
  COUNT(*) AS pv,
  COUNT(DISTINCT user_id) AS uv
FROM user_log
GROUP BY DATE_FORMAT(ts, 'yyyy-MM-dd HH:00');
```

* 使用到maven依赖如下

```xml 
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.48</version>
</dependency>

<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-sql-connector-kafka_2.12</artifactId>
    <version>1.12.0</version>
</dependency>

<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-jdbc_2.11</artifactId>
    <version>1.12.0</version>
</dependency>

<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-json</artifactId>
    <version>1.12.0</version>
</dependency>
```
* Kafka模拟发送的数据如下

```json 
{"user_id": "543462", "item_id":"1715", "category_id": "1464116", "behavior": "pv", "ts":"2021-02-01T01:00:00Z"}
{"user_id": "662867", "item_id":"2244074","category_id":"1575622","behavior": "pv", "ts":"2021-02-01T01:00:00Z"}
{"user_id": "662867", "item_id":"2244074","category_id":"1575622","behavior": "pv", "ts":"2021-02-01T01:00:00Z"}
{"user_id": "662867", "item_id":"2244074","category_id":"1575622","behavior": "learning flink", "ts":"2021-02-01T01:00:00Z"}
```

### 任务启动流程

任务启动流程图如下

<center>
<img src="http://assets.streamxhub.com/streamx-submit2.png?1234"/><br>
<strong>streamx-console 提交任务流程</strong>
</center>


关于项目的概念,`Development Mode`,`savepoint`,`NoteBook`,自定义jar管理,任务发布,任务恢复,参数配置,参数对比,多版本管理等等更多使用教程和文档后续持续更新...

