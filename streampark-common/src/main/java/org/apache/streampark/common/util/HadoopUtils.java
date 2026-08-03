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

package org.apache.streampark.common.util;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.InternalConfigHolder;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import javax.security.auth.kerberos.KerberosTicket;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/** Hadoop filesystem and YARN client utilities. */
public final class HadoopUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(HadoopUtils.class.getName());

    private static final String HADOOP_HOME = "HADOOP_HOME";
    private static final String HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
    private static final String CONF_SUFFIX = "/etc/hadoop";

    private static YarnClient reusableYarnClient;
    private static UserGroupInformation ugi;
    private static Configuration reusableConf;
    private static FileSystem reusableHdfs;
    private static KerberosTicket tgt;

    private static final Map<String, Configuration> CONFIGURATION_CACHE = new ConcurrentHashMap<>();

    private static String hadoopConfDir;
    private static Long tgtRefreshTime;

    private HadoopUtils() {
    }

    public static UserGroupInformation getUgi() {
        if (ugi == null) {
            if (HadoopConfigUtils.KERBEROS_ENABLE) {
                ugi = getKerberosUGI();
            } else {
                ugi = UserGroupInformation.createRemoteUser(HadoopConfigUtils.HADOOP_USER_NAME);
            }
        }
        return ugi;
    }

    private static String getHadoopConfDir() {
        if (hadoopConfDir == null) {
            try {
                hadoopConfDir = FileUtils.getPathFromEnv(HADOOP_CONF_DIR);
            } catch (Exception e) {
                try {
                    hadoopConfDir =
                        FileUtils.resolvePath(FileUtils.getPathFromEnv(HADOOP_HOME), CONF_SUFFIX);
                } catch (Exception ignored) {
                    hadoopConfDir = "";
                }
            }
        }
        return hadoopConfDir;
    }

    private static long getTgtRefreshTime() {
        if (tgtRefreshTime == null) {
            try {
                UserGroupInformation user = UserGroupInformation.getLoginUser();
                Method method = UserGroupInformation.class.getDeclaredMethod("getTGT");
                method.setAccessible(true);
                tgt = (KerberosTicket) method.invoke(user);
                if (tgt != null) {
                    long start = tgt.getStartTime().getTime();
                    long end = tgt.getEndTime().getTime();
                    tgtRefreshTime = (long) ((end - start) * 0.90f);
                } else {
                    LOG.warn("[StreamPark] get kerberos tgtRefreshTime failed, try get kerberos.ttl.");
                    DateUtils.TimeUnitPair timeUnit =
                        DateUtils.getTimeUnit(InternalConfigHolder.get(CommonConfig.KERBEROS_TTL()));
                    switch (timeUnit.unit) {
                        case SECONDS:
                            tgtRefreshTime = (long) timeUnit.num * 1000;
                            break;
                        case MINUTES:
                            tgtRefreshTime = (long) timeUnit.num * 60 * 1000;
                            break;
                        case HOURS:
                            tgtRefreshTime = (long) timeUnit.num * 60 * 60 * 1000;
                            break;
                        case DAYS:
                            tgtRefreshTime = (long) timeUnit.num * 60 * 60 * 24 * 1000;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                "[StreamPark] parameter:"
                                    + CommonConfig.KERBEROS_TTL().getKey()
                                    + " invalided, unit options are [s|m|h|d]");
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve kerberos TTL configuration", e);
            }
        }
        return tgtRefreshTime;
    }

    public static Configuration getConfigurationFromHadoopConfDir() {
        return getConfigurationFromHadoopConfDir(getHadoopConfDir());
    }

    public static Configuration getConfigurationFromHadoopConfDir(String confDir) {
        return CONFIGURATION_CACHE.computeIfAbsent(
            confDir,
            dir -> {
                FileUtils.exists(dir);
                File hadoopConfDirFile = new File(dir);
                List<String> confName =
                    Arrays.asList(
                        "hdfs-default.xml",
                        "core-site.xml",
                        "hdfs-site.xml",
                        "yarn-site.xml");
                File[] allFiles = hadoopConfDirFile.listFiles();
                HadoopConfiguration conf = new HadoopConfiguration();
                if (allFiles != null && CollectionUtils.isNotEmpty(Arrays.asList(allFiles))) {
                    for (File file : allFiles) {
                        if (file.isFile() && confName.contains(file.getName())) {
                            conf.addResource(new Path(file.getAbsolutePath()));
                        }
                    }
                }
                return conf;
            });
    }

    public static Configuration hadoopConf() {
        if (reusableConf == null) {
            reusableConf = getConfigurationFromHadoopConfDir(getHadoopConfDir());
            try {
                ClassLoaderUtils.loadResource(getHadoopConfDir());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load Hadoop resources from " + getHadoopConfDir(), e);
            }

            if (StringUtils.isBlank(reusableConf.get("hadoop.tmp.dir"))) {
                reusableConf.set("hadoop.tmp.dir", "/tmp");
            }
            if (StringUtils.isBlank(reusableConf.get("hbase.fs.tmp.dir"))) {
                reusableConf.set("hbase.fs.tmp.dir", "/tmp");
            }
            reusableConf.set("yarn.timeline-service.enabled", "false");
            reusableConf.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
            reusableConf.set("fs.file.impl", LocalFileSystem.class.getName());
            reusableConf.set("fs.hdfs.impl.disable.cache", "true");
        }
        return reusableConf;
    }

    private static void closeHadoop() {
        if (reusableHdfs != null) {
            try {
                reusableHdfs.close();
            } catch (IOException e) {
                LOG.warn("[StreamPark] close hdfs failed", e);
            }
            reusableHdfs = null;
        }
        if (reusableYarnClient != null) {
            try {
                reusableYarnClient.close();
            } catch (IOException e) {
                LOG.warn("[StreamPark] close yarn client failed", e);
            }
            reusableYarnClient = null;
        }
        if (tgt != null && !tgt.isDestroyed()) {
            try {
                tgt.destroy();
            } catch (javax.security.auth.DestroyFailedException e) {
                LOG.warn("[StreamPark] destroy kerberos ticket failed", e);
            }
            tgt = null;
        }
        reusableConf = null;
        ugi = null;
    }

    private static UserGroupInformation getKerberosUGI() {
        LOG.info("[StreamPark] kerberos login starting....");

        if (HadoopConfigUtils.KERBEROS_PRINCIPAL.isEmpty()
            || HadoopConfigUtils.KERBEROS_KEYTAB.isEmpty()) {
            throw new IllegalArgumentException(
                ConfigKeys.KEY_SECURITY_KERBEROS_PRINCIPAL()
                    + " and "
                    + ConfigKeys.KEY_SECURITY_KERBEROS_KEYTAB()
                    + " must not be empty");
        }

        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        if (!HadoopConfigUtils.KERBEROS_KRB5.isEmpty()) {
            System.setProperty("java.security.krb5.conf", HadoopConfigUtils.KERBEROS_KRB5);
            System.setProperty("java.security.krb5.conf.path", HadoopConfigUtils.KERBEROS_KRB5);
        }

        System.setProperty("sun.security.spnego.debug", HadoopConfigUtils.KERBEROS_DEBUG);
        System.setProperty("sun.security.krb5.debug", HadoopConfigUtils.KERBEROS_DEBUG);
        hadoopConf()
            .set(
                ConfigKeys.KEY_HADOOP_SECURITY_AUTHENTICATION(),
                ConfigKeys.KEY_KERBEROS());

        try {
            UserGroupInformation.setConfiguration(hadoopConf());
            UserGroupInformation kerberosUgi =
                UserGroupInformation.loginUserFromKeytabAndReturnUGI(
                    HadoopConfigUtils.KERBEROS_PRINCIPAL, HadoopConfigUtils.KERBEROS_KEYTAB);
            UserGroupInformation.setLoginUser(kerberosUgi);
            LOG.info("[StreamPark] kerberos authentication successful");
            return kerberosUgi;
        } catch (IOException e) {
            throw new IllegalStateException("Kerberos authentication failed", e);
        }
    }

    public static FileSystem hdfs() {
        if (reusableHdfs == null) {
            try {
                reusableHdfs =
                    getUgi()
                        .doAs(
                            (PrivilegedAction<FileSystem>) () -> {
                                try {
                                    return FileSystem.get(hadoopConf());
                                } catch (IOException e) {
                                    throw new IllegalStateException("Failed to obtain HDFS FileSystem", e);
                                }
                            });
                if (HadoopConfigUtils.KERBEROS_ENABLE) {
                    Timer timer = new Timer();
                    long refreshTime = getTgtRefreshTime();
                    timer.schedule(
                        new TimerTask() {

                            @Override
                            public void run() {
                                closeHadoop();
                                LOG.info(
                                    "[StreamPark] Check Kerberos Tgt And reLogin From Keytab Finish:refresh time: {}",
                                    DateUtils.format());
                            }
                        },
                        refreshTime,
                        refreshTime);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("[StreamPark] access hdfs error: " + e, e);
            }
        }
        return reusableHdfs;
    }

    public static YarnClient yarnClient() {
        if (reusableYarnClient == null || !reusableYarnClient.isInState(Service.STATE.STARTED)) {
            try {
                reusableYarnClient =
                    getUgi()
                        .doAs(
                            (PrivilegedAction<YarnClient>) () -> {
                                YarnConfiguration yarnConf =
                                    new YarnConfiguration(hadoopConf());
                                YarnClient client = YarnClient.createYarnClient();
                                client.init(yarnConf);
                                client.start();
                                return client;
                            });
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "[StreamPark] access yarnClient error: " + e, e);
            }
        }
        return reusableYarnClient;
    }

    public static ApplicationId toApplicationId(String appId) {
        if (appId == null) {
            throw new IllegalArgumentException(
                "[StreamPark] HadoopUtils.toApplicationId: applicationId muse not be null");
        }
        String[] timestampAndId = appId.split("_");
        return ApplicationId.newInstance(
            Long.parseLong(timestampAndId[1]), Integer.parseInt(timestampAndId[timestampAndId.length - 1]));
    }

    public static String downloadJar(String jarOnHdfs) throws IOException {
        File tmpDir = FileUtils.createTempDir();
        FileSystem fs = FileSystem.get(new Configuration());
        Path sourcePath = fs.makeQualified(new Path(jarOnHdfs));
        if (!fs.exists(sourcePath)) {
            throw new IOException("jar file: " + jarOnHdfs + " doesn't exist.");
        }
        Path destPath = new Path(tmpDir.getAbsolutePath() + "/" + sourcePath.getName());
        fs.copyToLocalFile(sourcePath, destPath);
        return new File(destPath.toString()).getAbsolutePath();
    }

    public static YarnApplicationState toYarnState(String state) {
        for (YarnApplicationState yarnState : YarnApplicationState.values()) {
            if (yarnState.name().equals(state)) {
                return yarnState;
            }
        }
        return null;
    }

    public static FinalApplicationStatus toYarnFinalStatus(String state) {
        for (FinalApplicationStatus status : FinalApplicationStatus.values()) {
            if (status.name().equals(state)) {
                return status;
            }
        }
        return null;
    }

    private static final class HadoopConfiguration extends Configuration {

        private static final List<String> REWRITE_NAMES =
            Arrays.asList(
                "dfs.blockreport.initialDelay",
                "dfs.datanode.directoryscan.interval",
                "dfs.heartbeat.interval",
                "dfs.namenode.decommission.interval",
                "dfs.namenode.replication.interval",
                "dfs.namenode.checkpoint.period",
                "dfs.namenode.checkpoint.check.period",
                "dfs.client.datanode-restart.timeout",
                "dfs.ha.log-roll.period",
                "dfs.ha.tail-edits.period",
                "dfs.datanode.bp-ready.timeout");

        private static String getHexDigits(String value) {
            boolean negative = false;
            String str = value;
            if (value.startsWith("-")) {
                negative = true;
                str = value.substring(1);
            }
            if (str.startsWith("0x") || str.startsWith("0X")) {
                String hexString = str.substring(2);
                if (negative) {
                    hexString = "-" + hexString;
                }
                return hexString;
            }
            return null;
        }

        private String getSafeValue(String name) {
            String value = getTrimmed(name);
            if (REWRITE_NAMES.contains(name)) {
                return value.replaceFirst("s$", "");
            }
            return value;
        }

        @Override
        public long getLong(String name, long defaultValue) {
            String valueString = getSafeValue(name);
            if (valueString == null) {
                return defaultValue;
            }
            String hexString = getHexDigits(valueString);
            if (hexString != null) {
                return Long.parseLong(hexString, 16);
            }
            return Long.parseLong(valueString);
        }
    }
}
