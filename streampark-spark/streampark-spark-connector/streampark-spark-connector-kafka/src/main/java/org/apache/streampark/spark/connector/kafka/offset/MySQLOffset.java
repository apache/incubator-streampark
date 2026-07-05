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

package org.apache.streampark.spark.connector.kafka.offset;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.JdbcUtils;

import org.apache.kafka.common.TopicPartition;
import org.apache.spark.SparkConf;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** MySQL offset manager. */
class MySQLOffset extends Offset {

    private final String jdbcURL;
    private final String table;
    private final String user;
    private final String password;
    private final Properties jdbcConfig;

    MySQLOffset(SparkConf sparkConf) {
        super(sparkConf);
        Map<String, String> params = getStoreParams();
        jdbcURL = params.get("mysql.jdbc.url");
        table = params.get("mysql.table");
        user = params.get("mysql.user");
        password = params.get("mysql.password");
        jdbcConfig = new Properties();
        jdbcConfig.setProperty(ConfigKeys.KEY_ALIAS(), "spark-kafka-offset");
        jdbcConfig.setProperty("jdbcUrl", jdbcURL);
        jdbcConfig.setProperty("username", user);
        jdbcConfig.setProperty("password", password);
    }

    @Override
    public Map<TopicPartition, Long> get(String groupId, Set<String> topics) {
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("topics must not be empty");
        }
        String where;
        if (topics.size() == 1) {
            where = " `topic` = \"" + topics.iterator().next() + "\" ";
        } else {
            where =
                    " `topic` in ("
                            + topics.stream()
                                    .map(t -> "\"" + t + "\"")
                                    .collect(Collectors.joining(","))
                            + ") ";
        }
        String sql =
                "select `topic`,`partition`,`offset` from "
                        + table
                        + " where `groupId`='"
                        + groupId
                        + "' and "
                        + where;
        List<Map<String, Object>> rows = JdbcUtils.select(sql, jdbcConfig);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<TopicPartition, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(
                    new TopicPartition(String.valueOf(row.get("topic")), Integer.parseInt(String.valueOf(row.get("partition")))),
                    Long.parseLong(String.valueOf(row.get("offset"))));
        }
        return result;
    }

    @Override
    public void update(String groupId, Map<TopicPartition, Long> offsetInfos) {
        for (Map.Entry<TopicPartition, Long> entry : offsetInfos.entrySet()) {
            TopicPartition tp = entry.getKey();
            String sql =
                    "insert into "
                            + table
                            + "(`topic`,`groupId`,`partition`,`offset`) values('"
                            + tp.topic()
                            + "','"
                            + groupId
                            + "','"
                            + tp.partition()
                            + "','"
                            + entry.getValue()
                            + "') on duplicate key update `offset`= values(`offset`) ";
            int updated = JdbcUtils.update(sql, jdbcConfig);
            if (updated == 0) {
                throw new RuntimeException("Commit kafka topic :" + tp.topic() + " failed!");
            }
        }
        log.info("storeType:MySQL,updateOffsets [ {},{} ]", groupId, offsetInfos);
    }

    @Override
    public void delete(String groupId, Set<String> topics) {
        for (String topic : topics) {
            JdbcUtils.update(
                    "delete from " + table + " where topic='" + topic + "' and groupId='" + groupId + "'",
                    jdbcConfig);
        }
        log.info("storeType:MySQL,deleteOffsets [ {},{} ]", groupId, topics);
    }
}
