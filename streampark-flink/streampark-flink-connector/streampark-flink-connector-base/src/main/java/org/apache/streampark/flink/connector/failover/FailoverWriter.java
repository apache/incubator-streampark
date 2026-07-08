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

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.JdbcUtils;
import org.apache.streampark.flink.connector.conf.FailoverStorageType;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/** Writes failed sink records to configured failover storage. */
public class FailoverWriter implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FailoverWriter.class);

    private static final class Lock {
        private static volatile boolean initialized = false;
        private static final ReentrantLock lock = new ReentrantLock();
    }

    private final FailoverStorageType failoverStorage;
    private final Properties properties;
    private KafkaProducer<String, String> kafkaProducer;

    public FailoverWriter(FailoverStorageType failoverStorage, Properties properties) {
        this.failoverStorage = failoverStorage;
        this.properties = properties;
    }

    public synchronized void write(SinkRequest request) {
        String table = request.getTable().split("\\.")[request.getTable().split("\\.").length - 1];
        switch (failoverStorage) {
            case NONE:
                break;
            case Console:
                List<String> records =
                        request.getRecords().stream()
                                .map(x -> "(" + cleanUp(x) + ")")
                                .collect(Collectors.toList());
                LOG.info("failover body: [ {} ]", String.join(",", records));
                break;
            case Kafka:
                initKafkaIfNeeded(table);
                String topic = properties.getProperty(ConfigKeys.KEY_KAFKA_TOPIC);
                long timestamp = System.currentTimeMillis();
                List<String> kafkaRecords =
                        request.getRecords().stream().map(this::cleanUp).collect(Collectors.toList());
                String sendData =
                        String.format(
                                "{\n\"values\":[%s],\n\"timestamp\":%d\n}\n",
                                String.join(",", kafkaRecords), timestamp);
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, sendData);
                try {
                    kafkaProducer
                            .send(
                                    record,
                                    new Callback() {
                                        @Override
                                        public void onCompletion(
                                                RecordMetadata recordMetadata, Exception e) {
                                            LOG.info(
                                                    "Failover successful!! storageType:Kafka,table: {},size:{}",
                                                    table,
                                                    request.size());
                                        }
                                    })
                            .get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case MySQL:
                initMySqlIfNeeded(table);
                long mysqlTimestamp = System.currentTimeMillis();
                List<String> mysqlRecords = new ArrayList<>();
                for (String x : request.getRecords()) {
                    String v = cleanUp(x);
                    mysqlRecords.add(String.format(" (%s,%d) ", v, mysqlTimestamp));
                }
                String sql =
                        String.format(
                                "INSERT INTO %s(`values`,`timestamp`) VALUES %s ",
                                table, String.join(",", mysqlRecords));
                JdbcUtils.update(sql, properties);
                LOG.info(
                        "Failover successful!! storageType:MySQL,table: {},size:{}",
                        table,
                        request.size());
                break;
            default:
                throw new UnsupportedOperationException(
                        "[StreamPark] unsupported failover storageType:" + failoverStorage);
        }
    }

    private void initKafkaIfNeeded(String table) {
        if (!Lock.initialized) {
            try {
                Lock.lock.lock();
                if (!Lock.initialized) {
                    Lock.initialized = true;
                    properties.put(
                            "key.serializer",
                            "org.apache.kafka.common.serialization.StringSerializer");
                    properties.put(
                            "value.serializer",
                            "org.apache.kafka.common.serialization.StringSerializer");
                    kafkaProducer = new KafkaProducer<>(properties);
                }
            } catch (Exception exception) {
                LOG.error("build Failover storageType:KAFKA failed", exception);
                throw exception;
            } finally {
                Lock.lock.unlock();
            }
        }
    }

    private void initMySqlIfNeeded(String table) {
        if (!Lock.initialized) {
            try {
                Lock.lock.lock();
                if (!Lock.initialized) {
                    Lock.initialized = true;
                    properties.put(ConfigKeys.KEY_ALIAS, "failover-" + table);
                    java.sql.Connection mysqlConnect = JdbcUtils.getConnection(properties);
                    java.sql.ResultSet mysqlTable =
                            mysqlConnect
                                    .getMetaData()
                                    .getTables(null, null, table, new String[] {"TABLE", "VIEW"});
                    if (!mysqlTable.next()) {
                        JdbcUtils.execute(
                                mysqlConnect,
                                String.format(
                                        "create table %s (`values` text, `timestamp` bigint)",
                                        table));
                        LOG.warn(
                                "Failover storageType:MySQL,table: {} is not exist,auto created...",
                                table);
                    }
                }
            } catch (Exception exception) {
                LOG.error("build Failover storageType:MySQL failed", exception);
                throw new RuntimeException(exception);
            } finally {
                Lock.lock.unlock();
            }
        }
    }

    private String cleanUp(String record) {
        return String.format(" \"%s\" ", record.replace("\"", "\\\""));
    }

    @Override
    public void close() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }
}
