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

    private ConfigKeys() {
    }

    public static final String PARAM_PREFIX = "--";

    /** about parameter... */
    public static final String KEY_APP_HOME = "app.home";

    public static final String KEY_HOST = "host";
    public static final String KEY_PORT = "port";
    public static final String KEY_DB = "db";
    public static final String KEY_USER = "user";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_TIMEOUT = "timeout";
    public static final String KEY_SEMANTIC = "semantic";

    /** kerberos */
    public static final String KEY_KERBEROS = "kerberos";

    public static final String KEY_KERBEROS_SERVICE_ACCOUNT = "kubernetes.service-account";
    public static final String KEY_HADOOP_USER_NAME = "HADOOP_USER_NAME";

    /** hadoop.security.authentication */
    public static final String KEY_HADOOP_SECURITY_AUTHENTICATION = "hadoop.security.authentication";

    public static final String KEY_SECURITY_KERBEROS_ENABLE = "security.kerberos.login.enable";
    public static final String KEY_SECURITY_KERBEROS_DEBUG = "security.kerberos.login.debug";
    public static final String KEY_SECURITY_KERBEROS_KEYTAB = "security.kerberos.login.keytab";
    public static final String KEY_SECURITY_KERBEROS_PRINCIPAL = "security.kerberos.login.principal";
    public static final String KEY_SECURITY_KERBEROS_KRB5_CONF = "security.kerberos.login.krb5";

    /** about spark */
    public static final String KEY_SPARK_MAIN_CLASS = "spark.main.class";
    public static final String KEY_SPARK_APP_NAME = "spark.app.name";
    public static final String KEY_SPARK_BATCH_DURATION = "spark.batch.duration";
    public static final String KEY_SPARK_DRIVER_CORES = "spark.driver.cores";
    public static final String KEY_SPARK_DRIVER_MEMORY = "spark.driver.memory";
    public static final String KEY_SPARK_EXECUTOR_INSTANCES = "spark.executor.instances";
    public static final String KEY_SPARK_EXECUTOR_CORES = "spark.executor.cores";
    public static final String KEY_SPARK_EXECUTOR_MEMORY = "spark.executor.memory";
    public static final String KEY_SPARK_DYNAMIC_ALLOCATION_ENABLED = "spark.dynamicAllocation.enabled";
    public static final String KEY_SPARK_DYNAMIC_ALLOCATION_MAX_EXECUTORS =
        "spark.dynamicAllocation.maxExecutors";
    public static final String KEY_SPARK_YARN_QUEUE = "spark.yarn.queue";
    public static final String KEY_SPARK_YARN_QUEUE_NAME = "yarnQueueName";
    public static final String KEY_SPARK_YARN_QUEUE_LABEL = "yarnQueueLabel";
    public static final String KEY_SPARK_YARN_AM_NODE_LABEL = "spark.yarn.am.nodeLabelExpression";
    public static final String KEY_SPARK_YARN_EXECUTOR_NODE_LABEL = "spark.yarn.executor.nodeLabelExpression";

    public static String keySparkSql(String prefix) {
        return (prefix != null ? prefix : "") + "sql";
    }

    /** Parameter key for Spark SQL script content (no CLI prefix). */
    public static final String KEY_SPARK_SQL = "sql";

    /** about config flink */
    public static String keyAppConf(String prefix) {
        return (prefix != null ? prefix : "") + "conf";
    }

    /** Parameter key for application config file path (no CLI prefix). */
    public static final String KEY_APP_CONF = "conf";

    public static String keyFlinkConf(String prefix) {
        return (prefix != null ? prefix : "") + "flink.conf";
    }

    /** Parameter key for Flink configuration (no CLI prefix). */
    public static final String KEY_FLINK_CONF = "flink.conf";

    public static String keyAppName(String prefix) {
        return (prefix != null ? prefix : "") + "app.name";
    }

    /** Parameter key for application name (no CLI prefix). */
    public static final String KEY_APP_NAME = "app.name";

    public static String keyFlinkSql(String prefix) {
        return (prefix != null ? prefix : "") + "sql";
    }

    /** Parameter key for Flink SQL script content (no CLI prefix). */
    public static final String KEY_FLINK_SQL = "sql";

    public static String keyFlinkParallelism(String prefix) {
        return (prefix != null ? prefix : "") + "parallelism.default";
    }

    /** Parameter key for default Flink parallelism (no CLI prefix). */
    public static final String KEY_FLINK_PARALLELISM = "parallelism.default";

    public static final String KEY_FLINK_OPTION_PREFIX = "flink.option.";
    public static final String KEY_FLINK_PROPERTY_PREFIX = "flink.property.";
    public static final String KEY_FLINK_TABLE_PREFIX = "flink.table.";
    public static final String KEY_SPARK_PROPERTY_PREFIX = "spark.";
    public static final String KEY_APP_PREFIX = "app.";
    public static final String KEY_SQL_PREFIX = "sql.";
    public static final String KEY_FLINK_APP_NAME = "pipeline.name";
    public static final String KEY_YARN_APP_ID = "yarn.application.id";
    public static final String KEY_YARN_APP_NAME = "yarn.application.name";
    public static final String KEY_YARN_APP_QUEUE = "yarn.application.queue";
    public static final String KEY_YARN_APP_NODE_LABEL = "yarn.application.node-label";
    public static final String KEY_K8S_IMAGE_PULL_POLICY = "kubernetes.container.image.pull-policy";
    public static final String FLINK_NATIVE_KUBERNETES_LABEL = "flink-native-kubernetes";

    /** about flink table */
    public static final String KEY_FLINK_TABLE_PLANNER = "flink.table.planner";
    public static final String KEY_FLINK_TABLE_MODE = "flink.table.mode";
    public static final String KEY_FLINK_TABLE_CATALOG = "flink.table.catalog";
    public static final String KEY_FLINK_TABLE_DATABASE = "flink.table.database";

    /** about config Kafka */
    public static final String KAFKA_SINK_PREFIX = "kafka.sink.";
    public static final String KAFKA_SOURCE_PREFIX = "kafka.source.";
    public static final String KEY_KAFKA_TOPIC = "topic";
    public static final String KEY_KAFKA_SEMANTIC = "semantic";
    public static final String KEY_KAFKA_PATTERN = "pattern";
    public static final String KEY_KAFKA_START_FROM = "start.from";
    public static final String KEY_KAFKA_START_FROM_OFFSET = "offset";
    public static final String KEY_KAFKA_START_FROM_TIMESTAMP = "timestamp";
    public static final String KEY_ALIAS = "alias";

    /** about config jdbc... */
    public static final String KEY_JDBC_PREFIX = "jdbc.";
    public static final String KEY_JDBC_DRIVER = "driverClassName";
    public static final String KEY_JDBC_URL = "jdbcUrl";
    public static final String KEY_JDBC_USER = "username";
    public static final String KEY_JDBC_PASSWORD = "password";
    public static final String KEY_JDBC_INSERT_BATCH = "batch.size";
    public static final int DEFAULT_JDBC_INSERT_BATCH = 1;
    public static final String MONGO_PREFIX = "mongodb.";

    /** about config HBase */
    public static final String HBASE_PREFIX = "hbase.";
    public static final String KEY_HBASE_COMMIT_BATCH = "hbase.commit.batch";
    public static final String KEY_HBASE_WRITE_SIZE = "hbase.client.write.size";
    public static final int DEFAULT_HBASE_COMMIT_BATCH = 1000;
    public static final String KEY_HBASE_AUTH_USER = "hbase.auth.user";
    public static final int DEFAULT_HBASE_WRITE_SIZE = 1024 * 1024 * 10;

    /** about influx */
    public static final String INFLUX_PREFIX = "influx.";
    public static final String KEY_FLINK_APPLICATION_MAIN_CLASS = "$internal.application.main";
    public static final String KEY_FLINK_JM_PROCESS_MEMORY = "jobmanager.memory.process.size";
    public static final String KEY_FLINK_TM_PROCESS_MEMORY = "taskmanager.memory.process.size";
}
