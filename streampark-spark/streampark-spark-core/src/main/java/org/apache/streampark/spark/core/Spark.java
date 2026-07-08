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

package org.apache.streampark.spark.core;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.spark.core.util.ParameterTool;
import org.apache.streampark.spark.core.util.SqlCommandCall;
import org.apache.streampark.spark.core.util.SqlCommandParser;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Base class for Spark applications. */
public abstract class Spark implements Serializable {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(Spark.class.getName());

    protected final transient SparkConf sparkConf = new SparkConf();
    private final transient List<String> sparkListeners = new ArrayList<>();
    protected transient SparkSession sparkSession;
    protected String checkpoint = "";
    protected boolean createOnError = true;
    private final ReentrantReadWriteLock.WriteLock lock = new ReentrantReadWriteLock().writeLock();

    public final void main(String[] args) {
        init(args);
        config(sparkConf);

        scala.collection.Iterator<scala.Tuple2<String, String>> sysProps =
            scala.collection.JavaConverters.asScalaIteratorConverter(
                java.util.Arrays.asList(
                    sparkConf.getAllWithPrefix("spark.config.system.properties"))
                    .iterator())
                .asScala()
                .toIterator();
        while (sysProps.hasNext()) {
            scala.Tuple2<String, String> x = sysProps.next();
            System.getProperties().setProperty(x._1().substring(1), x._2());
        }

        SparkSession.Builder builder = SparkSession.builder().config(sparkConf);
        boolean enableHive =
            sparkConf.getBoolean("spark.config.enable.hive.support", false);
        if (enableHive) {
            builder.enableHiveSupport();
        }
        sparkSession = builder.getOrCreate();

        scala.collection.Iterator<scala.Tuple2<String, String>> sparkSql =
            scala.collection.JavaConverters.asScalaIteratorConverter(
                java.util.Arrays.asList(
                    sparkConf.getAllWithPrefix("spark.config.spark.sql"))
                    .iterator())
                .asScala()
                .toIterator();
        while (sparkSql.hasNext()) {
            scala.Tuple2<String, String> x = sparkSql.next();
            sparkSession.sparkContext().getConf().set(x._1().substring(1), x._2());
        }

        ready();

        ParameterTool parameterTool = ParameterTool.fromArgs(args);
        String sparkSqls;
        String sql = parameterTool.get(ConfigKeys.KEY_SPARK_SQL());
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("Usage: spark sql cannot be null");
        }
        try {
            sparkSqls = DeflaterUtils.unzipString(sql);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Usage: spark sql is invalid or null, please check");
        }

        List<SqlCommandCall> commands = SqlCommandParser.parseSQL(sparkSqls);
        for (SqlCommandCall x : commands) {
            String args0 = x.operands.length == 0 ? null : x.operands[0];
            String command = x.command.getName();
            try {
                lock.lock();
                Dataset<Row> dataFrame = handle(x.originSql);
                LOG.info("{}:{}", command, args0);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        start();
        destroy();
    }

    private void init(String[] args) {
        List<String> argv = new ArrayList<>();
        for (String arg : args) {
            argv.add(arg);
        }
        String conf = null;
        List<Map.Entry<String, String>> userArgs = new ArrayList<>();

        int idx = 0;
        while (idx < argv.size()) {
            String current = argv.get(idx);
            if ("--conf".equals(current) && idx + 1 < argv.size()) {
                conf = argv.get(idx + 1);
                idx += 2;
            } else if ("--checkpoint".equals(current) && idx + 1 < argv.size()) {
                checkpoint = argv.get(idx + 1);
                idx += 2;
            } else if ("--createOnError".equals(current) && idx + 1 < argv.size()) {
                createOnError = Boolean.parseBoolean(argv.get(idx + 1));
                idx += 2;
            } else if (current.startsWith(ConfigKeys.PARAM_PREFIX()) && idx + 1 < argv.size()) {
                userArgs.add(
                    Map.entry(current.substring(ConfigKeys.PARAM_PREFIX().length()), argv.get(idx + 1)));
                idx += 2;
            } else if (current.startsWith(ConfigKeys.PARAM_PREFIX())) {
                LOG.error("Unrecognized options: {}", String.join(" ", argv.subList(idx, argv.size())));
                printUsageAndExit();
            } else {
                idx++;
            }
        }

        if (conf != null) {
            Map<String, String> localConf;
            String ext =
                conf.contains(".")
                    ? conf.substring(conf.lastIndexOf('.') + 1)
                    : "";
            switch (ext) {
                case "conf":
                    localConf = PropertiesUtils.fromHoconFile(conf);
                    break;
                case "properties":
                    localConf = PropertiesUtils.fromPropertiesFile(conf);
                    break;
                case "yaml":
                case "yml":
                    localConf = PropertiesUtils.fromYamlFile(conf);
                    break;
                default:
                    throw new IllegalArgumentException(
                        "[StreamPark] Usage: config file error,must be [properties|yaml|conf]");
            }
            localConf.forEach((k, v) -> sparkConf.set(k, v));
        }
        userArgs.forEach(e -> sparkConf.set(e.getKey(), e.getValue()));

        String appMain =
            sparkConf.get(
                ConfigKeys.KEY_SPARK_MAIN_CLASS(),
                "org.apache.streampark.spark.cli.SqlClient");
        if (appMain == null) {
            LOG.error(
                "[StreamPark] parameter: {} must not be empty!",
                ConfigKeys.KEY_SPARK_MAIN_CLASS());
            System.exit(1);
        }

        String appName = sparkConf.get(ConfigKeys.KEY_SPARK_APP_NAME(), null);
        if (appName == null || appName.isEmpty()) {
            appName = appMain;
        }

        if ("local".equals(sparkConf.get("spark.master", null))) {
            sparkConf.setAppName("[LocalDebug] " + appName).setMaster("local[*]");
            sparkConf.set("spark.streaming.kafka.maxRatePerPartition", "10");
        }
        sparkConf.set("spark.streaming.stopGracefullyOnShutdown", "true");

        String extraListeners =
            String.join(",", sparkListeners)
                + ","
                + sparkConf.get("spark.extraListeners", "");
        if (!extraListeners.equals(",")) {
            sparkConf.set("spark.extraListeners", extraListeners);
        }
    }

    protected void config(SparkConf sparkConf) {
    }

    protected void ready() {
    }

    protected Dataset<Row> handle(String sql) {
        return sparkSession.sql(sql);
    }

    protected void start() {
    }

    protected abstract void destroy();

    private void printUsageAndExit() {
        LOG.error(
            "\"Usage: Streaming [options]\\n\\n Options are:\\n   --checkpoint <checkpoint dir>\\n   --createOnError <Failed to recover from checkpoint, whether to recreated, true or false>\\n\"");
        System.exit(1);
    }
}
