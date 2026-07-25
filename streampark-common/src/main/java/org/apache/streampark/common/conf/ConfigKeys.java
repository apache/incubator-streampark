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

/**
 * Configuration key constants.
 *
 * <p>Parameterless {@code KEY_XXX()} methods mirror the original Scala API for downstream modules.
 */
@SuppressWarnings({"java:S100", "java:S3400"})
public final class ConfigKeys {

    private ConfigKeys() {
    }

    public static String PARAM_PREFIX() {
        return "--";
    }

    /** about parameter... */
    public static String KEY_APP_HOME() {
        return "app.home";
    }

    public static String KEY_HOST() {
        return "host";
    }

    public static String KEY_PORT() {
        return "port";
    }

    public static String KEY_DB() {
        return "db";
    }

    public static String KEY_USER() {
        return "user";
    }

    public static String KEY_PASSWORD() {
        return "password";
    }

    public static String KEY_TIMEOUT() {
        return "timeout";
    }

    public static String KEY_SEMANTIC() {
        return "semantic";
    }

    /** kerberos */
    public static String KEY_KERBEROS() {
        return "kerberos";
    }

    public static String KEY_KERBEROS_SERVICE_ACCOUNT() {
        return "kubernetes.service-account";
    }

    public static String KEY_HADOOP_USER_NAME() {
        return "HADOOP_USER_NAME";
    }

    /** hadoop.security.authentication */
    public static String KEY_HADOOP_SECURITY_AUTHENTICATION() {
        return "hadoop.security.authentication";
    }

    public static String KEY_SECURITY_KERBEROS_ENABLE() {
        return "security.kerberos.login.enable";
    }

    public static String KEY_SECURITY_KERBEROS_DEBUG() {
        return "security.kerberos.login.debug";
    }

    public static String KEY_SECURITY_KERBEROS_KEYTAB() {
        return "security.kerberos.login.keytab";
    }

    public static String KEY_SECURITY_KERBEROS_PRINCIPAL() {
        return "security.kerberos.login.principal";
    }

    public static String KEY_SECURITY_KERBEROS_KRB5_CONF() {
        return "security.kerberos.login.krb5";
    }

    /** about spark */
    public static String KEY_SPARK_MAIN_CLASS() {
        return "spark.main.class";
    }

    public static String KEY_SPARK_APP_NAME() {
        return "spark.app.name";
    }

    public static String KEY_SPARK_BATCH_DURATION() {
        return "spark.batch.duration";
    }

    public static String KEY_SPARK_DRIVER_CORES() {
        return "spark.driver.cores";
    }

    public static String KEY_SPARK_DRIVER_MEMORY() {
        return "spark.driver.memory";
    }

    public static String KEY_SPARK_EXECUTOR_INSTANCES() {
        return "spark.executor.instances";
    }

    public static String KEY_SPARK_EXECUTOR_CORES() {
        return "spark.executor.cores";
    }

    public static String KEY_SPARK_EXECUTOR_MEMORY() {
        return "spark.executor.memory";
    }

    public static String KEY_SPARK_DYNAMIC_ALLOCATION_ENABLED() {
        return "spark.dynamicAllocation.enabled";
    }

    public static String KEY_SPARK_DYNAMIC_ALLOCATION_MAX_EXECUTORS() {
        return "spark.dynamicAllocation.maxExecutors";
    }

    public static String KEY_SPARK_YARN_QUEUE() {
        return "spark.yarn.queue";
    }

    public static String KEY_SPARK_YARN_QUEUE_NAME() {
        return "yarnQueueName";
    }

    public static String KEY_SPARK_YARN_QUEUE_LABEL() {
        return "yarnQueueLabel";
    }

    public static String KEY_SPARK_YARN_AM_NODE_LABEL() {
        return "spark.yarn.am.nodeLabelExpression";
    }

    public static String KEY_SPARK_YARN_EXECUTOR_NODE_LABEL() {
        return "spark.yarn.executor.nodeLabelExpression";
    }

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

    public static String KEY_FLINK_OPTION_PREFIX() {
        return "flink.option.";
    }

    public static String KEY_FLINK_PROPERTY_PREFIX() {
        return "flink.property.";
    }

    public static String KEY_FLINK_TABLE_PREFIX() {
        return "flink.table.";
    }

    public static String KEY_SPARK_PROPERTY_PREFIX() {
        return "spark.";
    }

    public static String KEY_APP_PREFIX() {
        return "app.";
    }

    public static String KEY_SQL_PREFIX() {
        return "sql.";
    }

    public static String KEY_FLINK_APP_NAME() {
        return "pipeline.name";
    }

    public static String KEY_YARN_APP_ID() {
        return "yarn.application.id";
    }

    public static String KEY_YARN_APP_NAME() {
        return "yarn.application.name";
    }

    public static String KEY_YARN_APP_QUEUE() {
        return "yarn.application.queue";
    }

    public static String KEY_YARN_APP_NODE_LABEL() {
        return "yarn.application.node-label";
    }

    public static String KEY_K8S_IMAGE_PULL_POLICY() {
        return "kubernetes.container.image.pull-policy";
    }

    public static String FLINK_NATIVE_KUBERNETES_LABEL() {
        return "flink-native-kubernetes";
    }

    /** about flink table */
    public static String KEY_FLINK_TABLE_PLANNER() {
        return "flink.table.planner";
    }

    public static String KEY_FLINK_TABLE_MODE() {
        return "flink.table.mode";
    }

    public static String KEY_FLINK_TABLE_CATALOG() {
        return "flink.table.catalog";
    }

    public static String KEY_FLINK_TABLE_DATABASE() {
        return "flink.table.database";
    }

    /** about config Kafka */
    public static String KAFKA_SINK_PREFIX() {
        return "kafka.sink.";
    }

    public static String KAFKA_SOURCE_PREFIX() {
        return "kafka.source.";
    }

    public static String KEY_KAFKA_TOPIC() {
        return "topic";
    }

    public static String KEY_KAFKA_SEMANTIC() {
        return "semantic";
    }

    public static String KEY_KAFKA_PATTERN() {
        return "pattern";
    }

    public static String KEY_KAFKA_START_FROM() {
        return "start.from";
    }

    public static String KEY_KAFKA_START_FROM_OFFSET() {
        return "offset";
    }

    public static String KEY_KAFKA_START_FROM_TIMESTAMP() {
        return "timestamp";
    }

    public static String KEY_ALIAS() {
        return "alias";
    }

    /** about config jdbc... */
    public static String KEY_JDBC_PREFIX() {
        return "jdbc.";
    }

    public static String KEY_JDBC_DRIVER() {
        return "driverClassName";
    }

    public static String KEY_JDBC_URL() {
        return "jdbcUrl";
    }

    public static String KEY_JDBC_USER() {
        return "username";
    }

    public static String KEY_JDBC_PASSWORD() {
        return "password";
    }

    public static String KEY_JDBC_INSERT_BATCH() {
        return "batch.size";
    }

    public static int DEFAULT_JDBC_INSERT_BATCH() {
        return 1;
    }

    public static String MONGO_PREFIX() {
        return "mongodb.";
    }

    /** about config HBase */
    public static String HBASE_PREFIX() {
        return "hbase.";
    }

    public static String KEY_HBASE_COMMIT_BATCH() {
        return "hbase.commit.batch";
    }

    public static String KEY_HBASE_WRITE_SIZE() {
        return "hbase.client.write.size";
    }

    public static int DEFAULT_HBASE_COMMIT_BATCH() {
        return 1000;
    }

    public static int DEFAULT_HBASE_WRITE_SIZE() {
        return 1024 * 1024 * 10;
    }

    public static String KEY_HBASE_AUTH_USER() {
        return "hbase.auth.user";
    }

    /** about influx */
    public static String INFLUX_PREFIX() {
        return "influx.";
    }

    public static String KEY_FLINK_APPLICATION_MAIN_CLASS() {
        return "$internal.application.main";
    }

    public static String KEY_FLINK_JM_PROCESS_MEMORY() {
        return "jobmanager.memory.process.size";
    }

    public static String KEY_FLINK_TM_PROCESS_MEMORY() {
        return "taskmanager.memory.process.size";
    }
}
