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

package org.apache.streampark.common.conf;

/** Configuration key constants. */
public final class ConfigKeys {

    private ConfigKeys() {}

    private static final String PARAM_PREFIX_VALUE = "--";

    public static String PARAM_PREFIX() { return PARAM_PREFIX_VALUE; }

    /** about parameter... */
    private static final String KEY_APP_HOME_VALUE = "app.home";

    public static String KEY_APP_HOME() { return KEY_APP_HOME_VALUE; }

    private static final String KEY_HOST_VALUE = "host";

    public static String KEY_HOST() { return KEY_HOST_VALUE; }
    private static final String KEY_PORT_VALUE = "port";

    public static String KEY_PORT() { return KEY_PORT_VALUE; }
    private static final String KEY_DB_VALUE = "db";

    public static String KEY_DB() { return KEY_DB_VALUE; }
    private static final String KEY_USER_VALUE = "user";

    public static String KEY_USER() { return KEY_USER_VALUE; }
    private static final String KEY_PASSWORD_VALUE = "password";

    public static String KEY_PASSWORD() { return KEY_PASSWORD_VALUE; }
    private static final String KEY_TIMEOUT_VALUE = "timeout";

    public static String KEY_TIMEOUT() { return KEY_TIMEOUT_VALUE; }
    private static final String KEY_SEMANTIC_VALUE = "semantic";

    public static String KEY_SEMANTIC() { return KEY_SEMANTIC_VALUE; }

    /** kerberos */
    private static final String KEY_KERBEROS_VALUE = "kerberos";

    public static String KEY_KERBEROS() { return KEY_KERBEROS_VALUE; }

    private static final String KEY_KERBEROS_SERVICE_ACCOUNT_VALUE = "kubernetes.service-account";

    public static String KEY_KERBEROS_SERVICE_ACCOUNT() { return KEY_KERBEROS_SERVICE_ACCOUNT_VALUE; }
    private static final String KEY_HADOOP_USER_NAME_VALUE = "HADOOP_USER_NAME";

    public static String KEY_HADOOP_USER_NAME() { return KEY_HADOOP_USER_NAME_VALUE; }

    /** hadoop.security.authentication */
    private static final String KEY_HADOOP_SECURITY_AUTHENTICATION_VALUE = "hadoop.security.authentication";

    public static String KEY_HADOOP_SECURITY_AUTHENTICATION() { return KEY_HADOOP_SECURITY_AUTHENTICATION_VALUE; }

    private static final String KEY_SECURITY_KERBEROS_ENABLE_VALUE = "security.kerberos.login.enable";

    public static String KEY_SECURITY_KERBEROS_ENABLE() { return KEY_SECURITY_KERBEROS_ENABLE_VALUE; }
    private static final String KEY_SECURITY_KERBEROS_DEBUG_VALUE = "security.kerberos.login.debug";

    public static String KEY_SECURITY_KERBEROS_DEBUG() { return KEY_SECURITY_KERBEROS_DEBUG_VALUE; }
    private static final String KEY_SECURITY_KERBEROS_KEYTAB_VALUE = "security.kerberos.login.keytab";

    public static String KEY_SECURITY_KERBEROS_KEYTAB() { return KEY_SECURITY_KERBEROS_KEYTAB_VALUE; }
    private static final String KEY_SECURITY_KERBEROS_PRINCIPAL_VALUE = "security.kerberos.login.principal";

    public static String KEY_SECURITY_KERBEROS_PRINCIPAL() { return KEY_SECURITY_KERBEROS_PRINCIPAL_VALUE; }
    private static final String KEY_SECURITY_KERBEROS_KRB5_CONF_VALUE = "security.kerberos.login.krb5";

    public static String KEY_SECURITY_KERBEROS_KRB5_CONF() { return KEY_SECURITY_KERBEROS_KRB5_CONF_VALUE; }

    /** about spark */
    private static final String KEY_SPARK_MAIN_CLASS_VALUE = "spark.main.class";

    public static String KEY_SPARK_MAIN_CLASS() { return KEY_SPARK_MAIN_CLASS_VALUE; }
    private static final String KEY_SPARK_APP_NAME_VALUE = "spark.app.name";

    public static String KEY_SPARK_APP_NAME() { return KEY_SPARK_APP_NAME_VALUE; }
    private static final String KEY_SPARK_BATCH_DURATION_VALUE = "spark.batch.duration";

    public static String KEY_SPARK_BATCH_DURATION() { return KEY_SPARK_BATCH_DURATION_VALUE; }
    private static final String KEY_SPARK_DRIVER_CORES_VALUE = "spark.driver.cores";

    public static String KEY_SPARK_DRIVER_CORES() { return KEY_SPARK_DRIVER_CORES_VALUE; }
    private static final String KEY_SPARK_DRIVER_MEMORY_VALUE = "spark.driver.memory";

    public static String KEY_SPARK_DRIVER_MEMORY() { return KEY_SPARK_DRIVER_MEMORY_VALUE; }
    private static final String KEY_SPARK_EXECUTOR_INSTANCES_VALUE = "spark.executor.instances";

    public static String KEY_SPARK_EXECUTOR_INSTANCES() { return KEY_SPARK_EXECUTOR_INSTANCES_VALUE; }
    private static final String KEY_SPARK_EXECUTOR_CORES_VALUE = "spark.executor.cores";

    public static String KEY_SPARK_EXECUTOR_CORES() { return KEY_SPARK_EXECUTOR_CORES_VALUE; }
    private static final String KEY_SPARK_EXECUTOR_MEMORY_VALUE = "spark.executor.memory";

    public static String KEY_SPARK_EXECUTOR_MEMORY() { return KEY_SPARK_EXECUTOR_MEMORY_VALUE; }
    private static final String KEY_SPARK_DYNAMIC_ALLOCATION_ENABLED_VALUE = "spark.dynamicAllocation.enabled";

    public static String KEY_SPARK_DYNAMIC_ALLOCATION_ENABLED() { return KEY_SPARK_DYNAMIC_ALLOCATION_ENABLED_VALUE; }
    private static final String KEY_SPARK_DYNAMIC_ALLOCATION_MAX_EXECUTORS_VALUE = "spark.dynamicAllocation.maxExecutors";

    public static String KEY_SPARK_DYNAMIC_ALLOCATION_MAX_EXECUTORS() { return KEY_SPARK_DYNAMIC_ALLOCATION_MAX_EXECUTORS_VALUE; }
    private static final String KEY_SPARK_YARN_QUEUE_VALUE = "spark.yarn.queue";

    public static String KEY_SPARK_YARN_QUEUE() { return KEY_SPARK_YARN_QUEUE_VALUE; }
    private static final String KEY_SPARK_YARN_QUEUE_NAME_VALUE = "yarnQueueName";

    public static String KEY_SPARK_YARN_QUEUE_NAME() { return KEY_SPARK_YARN_QUEUE_NAME_VALUE; }
    private static final String KEY_SPARK_YARN_QUEUE_LABEL_VALUE = "yarnQueueLabel";

    public static String KEY_SPARK_YARN_QUEUE_LABEL() { return KEY_SPARK_YARN_QUEUE_LABEL_VALUE; }
    private static final String KEY_SPARK_YARN_AM_NODE_LABEL_VALUE = "spark.yarn.am.nodeLabelExpression";

    public static String KEY_SPARK_YARN_AM_NODE_LABEL() { return KEY_SPARK_YARN_AM_NODE_LABEL_VALUE; }
    private static final String KEY_SPARK_YARN_EXECUTOR_NODE_LABEL_VALUE = "spark.yarn.executor.nodeLabelExpression";

    public static String KEY_SPARK_YARN_EXECUTOR_NODE_LABEL() { return KEY_SPARK_YARN_EXECUTOR_NODE_LABEL_VALUE; }

    public static String KEY_SPARK_SQL() {
        return KEY_SPARK_SQL(null);
    }

    public static String KEY_SPARK_SQL(String prefix) {
        return (prefix != null ? prefix : "") + "sql";
    }

    /** about config flink */
    public static String KEY_APP_CONF() {
        return KEY_APP_CONF(null);
    }

    public static String KEY_APP_CONF(String prefix) {
        return (prefix != null ? prefix : "") + "conf";
    }

    public static String KEY_FLINK_CONF() {
        return KEY_FLINK_CONF(null);
    }

    public static String KEY_FLINK_CONF(String prefix) {
        return (prefix != null ? prefix : "") + "flink.conf";
    }

    public static String KEY_APP_NAME() {
        return KEY_APP_NAME(null);
    }

    public static String KEY_APP_NAME(String prefix) {
        return (prefix != null ? prefix : "") + "app.name";
    }

    public static String KEY_FLINK_SQL() {
        return KEY_FLINK_SQL(null);
    }

    public static String KEY_FLINK_SQL(String prefix) {
        return (prefix != null ? prefix : "") + "sql";
    }

    public static String KEY_FLINK_PARALLELISM() {
        return KEY_FLINK_PARALLELISM(null);
    }

    public static String KEY_FLINK_PARALLELISM(String prefix) {
        return (prefix != null ? prefix : "") + "parallelism.default";
    }

    private static final String KEY_FLINK_OPTION_PREFIX_VALUE = "flink.option.";

    public static String KEY_FLINK_OPTION_PREFIX() { return KEY_FLINK_OPTION_PREFIX_VALUE; }
    private static final String KEY_FLINK_PROPERTY_PREFIX_VALUE = "flink.property.";

    public static String KEY_FLINK_PROPERTY_PREFIX() { return KEY_FLINK_PROPERTY_PREFIX_VALUE; }
    private static final String KEY_FLINK_TABLE_PREFIX_VALUE = "flink.table.";

    public static String KEY_FLINK_TABLE_PREFIX() { return KEY_FLINK_TABLE_PREFIX_VALUE; }
    private static final String KEY_SPARK_PROPERTY_PREFIX_VALUE = "spark.";

    public static String KEY_SPARK_PROPERTY_PREFIX() { return KEY_SPARK_PROPERTY_PREFIX_VALUE; }
    private static final String KEY_APP_PREFIX_VALUE = "app.";

    public static String KEY_APP_PREFIX() { return KEY_APP_PREFIX_VALUE; }
    private static final String KEY_SQL_PREFIX_VALUE = "sql.";

    public static String KEY_SQL_PREFIX() { return KEY_SQL_PREFIX_VALUE; }
    private static final String KEY_FLINK_APP_NAME_VALUE = "pipeline.name";

    public static String KEY_FLINK_APP_NAME() { return KEY_FLINK_APP_NAME_VALUE; }
    private static final String KEY_YARN_APP_ID_VALUE = "yarn.application.id";

    public static String KEY_YARN_APP_ID() { return KEY_YARN_APP_ID_VALUE; }
    private static final String KEY_YARN_APP_NAME_VALUE = "yarn.application.name";

    public static String KEY_YARN_APP_NAME() { return KEY_YARN_APP_NAME_VALUE; }
    private static final String KEY_YARN_APP_QUEUE_VALUE = "yarn.application.queue";

    public static String KEY_YARN_APP_QUEUE() { return KEY_YARN_APP_QUEUE_VALUE; }
    private static final String KEY_YARN_APP_NODE_LABEL_VALUE = "yarn.application.node-label";

    public static String KEY_YARN_APP_NODE_LABEL() { return KEY_YARN_APP_NODE_LABEL_VALUE; }
    private static final String KEY_K8S_IMAGE_PULL_POLICY_VALUE = "kubernetes.container.image.pull-policy";

    public static String KEY_K8S_IMAGE_PULL_POLICY() { return KEY_K8S_IMAGE_PULL_POLICY_VALUE; }
    private static final String FLINK_NATIVE_KUBERNETES_LABEL_VALUE = "flink-native-kubernetes";

    public static String FLINK_NATIVE_KUBERNETES_LABEL() { return FLINK_NATIVE_KUBERNETES_LABEL_VALUE; }

    /** about flink table */
    private static final String KEY_FLINK_TABLE_PLANNER_VALUE = "flink.table.planner";

    public static String KEY_FLINK_TABLE_PLANNER() { return KEY_FLINK_TABLE_PLANNER_VALUE; }
    private static final String KEY_FLINK_TABLE_MODE_VALUE = "flink.table.mode";

    public static String KEY_FLINK_TABLE_MODE() { return KEY_FLINK_TABLE_MODE_VALUE; }
    private static final String KEY_FLINK_TABLE_CATALOG_VALUE = "flink.table.catalog";

    public static String KEY_FLINK_TABLE_CATALOG() { return KEY_FLINK_TABLE_CATALOG_VALUE; }
    private static final String KEY_FLINK_TABLE_DATABASE_VALUE = "flink.table.database";

    public static String KEY_FLINK_TABLE_DATABASE() { return KEY_FLINK_TABLE_DATABASE_VALUE; }

    /** about config Kafka */
    private static final String KAFKA_SINK_PREFIX_VALUE = "kafka.sink.";

    public static String KAFKA_SINK_PREFIX() { return KAFKA_SINK_PREFIX_VALUE; }
    private static final String KAFKA_SOURCE_PREFIX_VALUE = "kafka.source.";

    public static String KAFKA_SOURCE_PREFIX() { return KAFKA_SOURCE_PREFIX_VALUE; }
    private static final String KEY_KAFKA_TOPIC_VALUE = "topic";

    public static String KEY_KAFKA_TOPIC() { return KEY_KAFKA_TOPIC_VALUE; }
    private static final String KEY_KAFKA_SEMANTIC_VALUE = "semantic";

    public static String KEY_KAFKA_SEMANTIC() { return KEY_KAFKA_SEMANTIC_VALUE; }
    private static final String KEY_KAFKA_PATTERN_VALUE = "pattern";

    public static String KEY_KAFKA_PATTERN() { return KEY_KAFKA_PATTERN_VALUE; }
    private static final String KEY_KAFKA_START_FROM_VALUE = "start.from";

    public static String KEY_KAFKA_START_FROM() { return KEY_KAFKA_START_FROM_VALUE; }
    private static final String KEY_KAFKA_START_FROM_OFFSET_VALUE = "offset";

    public static String KEY_KAFKA_START_FROM_OFFSET() { return KEY_KAFKA_START_FROM_OFFSET_VALUE; }
    private static final String KEY_KAFKA_START_FROM_TIMESTAMP_VALUE = "timestamp";

    public static String KEY_KAFKA_START_FROM_TIMESTAMP() { return KEY_KAFKA_START_FROM_TIMESTAMP_VALUE; }
    private static final String KEY_ALIAS_VALUE = "alias";

    public static String KEY_ALIAS() { return KEY_ALIAS_VALUE; }

    /** about config jdbc... */
    private static final String KEY_JDBC_PREFIX_VALUE = "jdbc.";

    public static String KEY_JDBC_PREFIX() { return KEY_JDBC_PREFIX_VALUE; }
    private static final String KEY_JDBC_DRIVER_VALUE = "driverClassName";

    public static String KEY_JDBC_DRIVER() { return KEY_JDBC_DRIVER_VALUE; }
    private static final String KEY_JDBC_URL_VALUE = "jdbcUrl";

    public static String KEY_JDBC_URL() { return KEY_JDBC_URL_VALUE; }
    private static final String KEY_JDBC_USER_VALUE = "username";

    public static String KEY_JDBC_USER() { return KEY_JDBC_USER_VALUE; }
    private static final String KEY_JDBC_PASSWORD_VALUE = "password";

    public static String KEY_JDBC_PASSWORD() { return KEY_JDBC_PASSWORD_VALUE; }
    private static final String KEY_JDBC_INSERT_BATCH_VALUE = "batch.size";

    public static String KEY_JDBC_INSERT_BATCH() { return KEY_JDBC_INSERT_BATCH_VALUE; }
    public static final int DEFAULT_JDBC_INSERT_BATCH = 1;
    private static final String MONGO_PREFIX_VALUE = "mongodb.";

    public static String MONGO_PREFIX() { return MONGO_PREFIX_VALUE; }

    /** about config HBase */
    private static final String HBASE_PREFIX_VALUE = "hbase.";

    public static String HBASE_PREFIX() { return HBASE_PREFIX_VALUE; }
    private static final String KEY_HBASE_COMMIT_BATCH_VALUE = "hbase.commit.batch";

    public static String KEY_HBASE_COMMIT_BATCH() { return KEY_HBASE_COMMIT_BATCH_VALUE; }
    private static final String KEY_HBASE_WRITE_SIZE_VALUE = "hbase.client.write.size";

    public static String KEY_HBASE_WRITE_SIZE() { return KEY_HBASE_WRITE_SIZE_VALUE; }
    public static final int DEFAULT_HBASE_COMMIT_BATCH = 1000;
    private static final String KEY_HBASE_AUTH_USER_VALUE = "hbase.auth.user";

    public static String KEY_HBASE_AUTH_USER() { return KEY_HBASE_AUTH_USER_VALUE; }
    public static final int DEFAULT_HBASE_WRITE_SIZE = 1024 * 1024 * 10;

    /** about influx */
    private static final String INFLUX_PREFIX_VALUE = "influx.";

    public static String INFLUX_PREFIX() { return INFLUX_PREFIX_VALUE; }
    private static final String KEY_FLINK_APPLICATION_MAIN_CLASS_VALUE = "$internal.application.main";

    public static String KEY_FLINK_APPLICATION_MAIN_CLASS() { return KEY_FLINK_APPLICATION_MAIN_CLASS_VALUE; }
    private static final String KEY_FLINK_JM_PROCESS_MEMORY_VALUE = "jobmanager.memory.process.size";

    public static String KEY_FLINK_JM_PROCESS_MEMORY() { return KEY_FLINK_JM_PROCESS_MEMORY_VALUE; }
    private static final String KEY_FLINK_TM_PROCESS_MEMORY_VALUE = "taskmanager.memory.process.size";

    public static String KEY_FLINK_TM_PROCESS_MEMORY() { return KEY_FLINK_TM_PROCESS_MEMORY_VALUE; }
}
