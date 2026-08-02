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

package org.apache.streampark.spark.core.util;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.PropertiesUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Seconds;
import org.apache.spark.streaming.StreamingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Executes Spark SQL scripts in batch or streaming mode. */
public final class SparkSqlExecutor {

    private static final ReentrantReadWriteLock.WriteLock LOCK =
        new ReentrantReadWriteLock().writeLock();

    private static final Log LOG = new Log();

    private SparkSqlExecutor() {
    }

    public static void runBatch(String[] args) {
        SparkSession sparkSession = createSparkSession(args);
        try {
            executeSql(args, sparkSession);
        } finally {
            sparkSession.sparkContext().stop();
        }
    }

    public static void runStreaming(String[] args) {
        SparkSession sparkSession = createSparkSession(args);
        SparkConf sparkConf = sparkSession.sparkContext().getConf();
        String checkpoint = resolveOption(args, "--checkpoint");
        boolean createOnError = resolveBooleanOption(args, "--createOnError", true);

        StreamingContext context;
        if (checkpoint.isEmpty()) {
            context =
                new StreamingContext(
                    sparkSession.sparkContext(),
                    Seconds.apply(sparkConf.getInt(ConfigKeys.KEY_SPARK_BATCH_DURATION(), 1)));
        } else {
            context =
                StreamingContext.getOrCreate(
                    checkpoint,
                    () -> new StreamingContext(
                        sparkSession.sparkContext(),
                        Seconds.apply(sparkConf.getInt(ConfigKeys.KEY_SPARK_BATCH_DURATION(), 1))),
                    null,
                    createOnError);
            context.checkpoint(checkpoint);
        }

        try {
            executeSql(args, sparkSession);
            context.start();
            context.awaitTermination();
        } finally {
            context.stop(false, true);
        }
    }

    private static SparkSession createSparkSession(String[] args) {
        SparkConf sparkConf = initSparkConf(args);
        SparkSession.Builder builder = SparkSession.builder().config(sparkConf);
        if (sparkConf.getBoolean("spark.config.enable.hive.support", false)) {
            builder.enableHiveSupport();
        }
        return builder.getOrCreate();
    }

    private static void executeSql(String[] args, SparkSession sparkSession) {
        Map<String, String> parameterTool = ParameterTool.fromArgs(args);
        String sql = parameterTool.get(ConfigKeys.KEY_SPARK_SQL());
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("Usage: spark sql cannot be null");
        }
        String sparkSql;
        try {
            sparkSql = DeflaterUtils.unzipString(sql);
        } catch (Exception e) {
            throw new IllegalArgumentException("Usage: spark sql is invalid or null, please check");
        }

        List<SqlCommandCall> commands = SqlCommandParser.parseSQL(sparkSql);
        for (SqlCommandCall command : commands) {
            String operands =
                command.operands().length == 0 ? null : command.operands()[0];
            try {
                LOCK.lock();
                sparkSession.sql(command.originSql());
                LOG.info(command.command().commandName() + ":" + operands);
            } finally {
                if (LOCK.isHeldByCurrentThread()) {
                    LOCK.unlock();
                }
            }
        }
    }

    private static SparkConf initSparkConf(String[] args) {
        CliArgs cliArgs = parseCliArgs(args);
        SparkConf sparkConf = new SparkConf();
        applyConfFile(cliArgs.confPath, sparkConf);
        applyUserArgs(cliArgs.userArgs, sparkConf);
        applyAppDefaults(sparkConf);
        return sparkConf;
    }

    private static CliArgs parseCliArgs(String[] args) {
        CliArgs cliArgs = new CliArgs();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--conf".equals(arg) && i + 1 < args.length) {
                cliArgs.confPath = args[++i];
            } else if ("--checkpoint".equals(arg) && i + 1 < args.length) {
                i++;
            } else if ("--createOnError".equals(arg) && i + 1 < args.length) {
                i++;
            } else if (arg.startsWith(ConfigKeys.PARAM_PREFIX()) && i + 1 < args.length) {
                cliArgs.userArgs.add(new String[]{arg.substring(2), args[++i]});
            } else if (!arg.startsWith("-")) {
                LOG.error("Unrecognized options: " + arg);
                printUsageAndExit();
            }
        }
        return cliArgs;
    }

    private static void applyConfFile(String conf, SparkConf sparkConf) {
        if (conf == null) {
            return;
        }
        Map<String, String> localConf;
        String extension = conf.substring(conf.lastIndexOf('.') + 1);
        switch (extension) {
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
        localConf.forEach(sparkConf::set);
    }

    private static void applyUserArgs(List<String[]> userArgs, SparkConf sparkConf) {
        for (String[] arg : userArgs) {
            sparkConf.set(arg[0], arg[1]);
        }
    }

    private static void applyAppDefaults(SparkConf sparkConf) {
        String appMain =
            sparkConf.get(ConfigKeys.KEY_SPARK_MAIN_CLASS(), "org.apache.streampark.spark.cli.SqlClient");
        if (appMain == null) {
            LOG.error("[StreamPark] parameter: " + ConfigKeys.KEY_SPARK_MAIN_CLASS() + " must not be empty!");
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
    }

    private static String resolveOption(String[] args, String optionName) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (optionName.equals(args[i])) {
                return args[i + 1];
            }
        }
        return "";
    }

    private static boolean resolveBooleanOption(String[] args, String optionName, boolean defaultValue) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (optionName.equals(args[i])) {
                return Boolean.parseBoolean(args[i + 1]);
            }
        }
        return defaultValue;
    }

    private static void printUsageAndExit() {
        LOG.error(
            "\"Usage: Streaming [options]\n"
                + "\n"
                + " Options are:\n"
                + "   --checkpoint <checkpoint dir>\n"
                + "   --createOnError <Failed to recover from checkpoint,"
                + " whether to recreated, true or false>\n");
        System.exit(1);
    }

    private static final class CliArgs {

        private String confPath;
        private final List<String[]> userArgs = new ArrayList<>();
    }

    private static final class Log extends LoggerSupport {

        void info(String msg) {
            logInfo(msg);
        }

        void error(String msg) {
            logError(msg);
        }
    }
}
