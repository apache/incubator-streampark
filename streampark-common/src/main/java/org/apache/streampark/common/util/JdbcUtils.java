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

import org.apache.streampark.common.conf.ConfigKeys;

import org.apache.streampark.shaded.org.slf4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Based on the hikari connection pool implementation. Support multiple data sources, note that all
 * modifications and additions are automatically committed transactions.
 */
public final class JdbcUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(JdbcUtils.class.getName());

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, HikariDataSource> DATA_SOURCE_HOLDER =
        new ConcurrentHashMap<>();

    private JdbcUtils() {
    }

    public static List<Map<String, Object>> select(String sql, Properties jdbcConfig) {
        return select(sql, null, jdbcConfig);
    }

    public static List<Map<String, Object>> select(
                                                   String sql, Consumer<ResultSet> func, Properties jdbcConfig) {
        if (sql == null || sql.isEmpty()) {
            return Collections.emptyList();
        }
        Connection conn = getConnection(jdbcConfig);
        Statement stmt = null;
        ResultSet result = null;
        try {
            stmt = createStatement(conn);
            result = stmt.executeQuery(sql);
            if (func != null) {
                func.accept(result);
            }
            int count = result.getMetaData().getColumnCount();
            List<Map<String, Object>> array = new ArrayList<>();
            while (result.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int x = 1; x <= count; x++) {
                    String key = result.getMetaData().getColumnLabel(x);
                    Object value = result.getObject(x);
                    map.put(key, value);
                }
                array.add(map);
            }
            return array.isEmpty() ? Collections.emptyList() : array;
        } catch (Exception ex) {
            LOG.warn("Failed to execute JDBC select", ex);
            return Collections.emptyList();
        } finally {
            close(result, stmt, conn);
        }
    }

    public static long count(String sql, Properties jdbcConfig) {
        Map<String, Object> row = unique(sql, jdbcConfig);
        if (row.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(row.values().iterator().next().toString());
    }

    public static long count(Connection conn, String sql) {
        Map<String, Object> row = unique(conn, sql);
        if (row.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(row.values().iterator().next().toString());
    }

    public static int batch(Iterable<String> sql, Properties jdbcConfig) {
        int size = 0;
        for (String ignored : sql) {
            size++;
        }
        if (size == 0) {
            return 0;
        }
        if (size == 1) {
            return update(sql.iterator().next(), jdbcConfig);
        }
        Connection conn = getConnection(jdbcConfig);
        try {
            Statement prepStat = conn.createStatement();
            try {
                int index = 0;
                int batchSize = 1000;
                int total = 0;
                for (String x : sql) {
                    prepStat.addBatch(x);
                    index++;
                    if (index > 0 && index % batchSize == 0) {
                        int count = 0;
                        for (int c : prepStat.executeBatch()) {
                            count += c;
                        }
                        conn.commit();
                        prepStat.clearBatch();
                        total += count;
                    }
                }
                for (int c : prepStat.executeBatch()) {
                    total += c;
                }
                return total;
            } catch (Exception ex) {
                LOG.warn("Failed to execute JDBC select", ex);
                return 0;
            } finally {
                conn.commit();
                close(conn);
            }
        } catch (Exception ex) {
            LOG.warn("Failed to execute JDBC select", ex);
            return 0;
        }
    }

    public static int update(String sql, Properties jdbcConfig) {
        return update(getConnection(jdbcConfig), sql);
    }

    public static int update(Connection conn, String sql) {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            return statement.executeUpdate(sql);
        } catch (Exception ex) {
            LOG.warn("Failed to execute JDBC select", ex);
            return -1;
        } finally {
            close(statement, conn);
        }
    }

    public static Map<String, Object> unique(String sql, Properties jdbcConfig) {
        return unique(getConnection(jdbcConfig), sql);
    }

    public static Map<String, Object> unique(Connection conn, String sql) {
        Statement stmt = null;
        ResultSet result = null;
        try {
            stmt = createStatement(conn);
            result = stmt.executeQuery(sql);
            int count = result.getMetaData().getColumnCount();
            if (!result.next()) {
                return Collections.emptyMap();
            }
            Map<String, Object> map = new HashMap<>();
            for (int x = 1; x <= count; x++) {
                String key = result.getMetaData().getColumnLabel(x);
                Object value = result.getObject(x);
                map.put(key, value);
            }
            return map;
        } catch (Exception ex) {
            LOG.warn("Failed to execute JDBC select", ex);
            return Collections.emptyMap();
        } finally {
            close(result, stmt, conn);
        }
    }

    public static boolean execute(String sql, Properties jdbcConfig) {
        return execute(getConnection(jdbcConfig), sql);
    }

    public static boolean execute(Connection conn, String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return stmt.execute(sql);
        } catch (Exception ex) {
            LOG.warn("Failed to execute JDBC select", ex);
            return false;
        } finally {
            close(stmt, conn);
        }
    }

    public static Connection getConnection(Properties prop) {
        String alias = prop.getProperty(ConfigKeys.KEY_ALIAS());
        ReentrantLock lock = LOCK_MAP.computeIfAbsent(alias, k -> new ReentrantLock());
        lock.lock();
        try {
            HikariDataSource ds = DATA_SOURCE_HOLDER.get(alias);
            if (ds == null) {
                HikariConfig jdbcConfig = new HikariConfig();
                for (String key : prop.stringPropertyNames()) {
                    if (ConfigKeys.KEY_ALIAS().equals(key) || ConfigKeys.KEY_SEMANTIC().equals(key)) {
                        continue;
                    }
                    String value = prop.getProperty(key);
                    try {
                        Field field = jdbcConfig.getClass().getDeclaredField(key);
                        field.setAccessible(true);
                        setFieldValue(field, jdbcConfig, value);
                    } catch (NoSuchFieldException e) {
                        String setMethod =
                            "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                        Method method = null;
                        for (Method m : jdbcConfig.getClass().getMethods()) {
                            if (m.getName().equals(setMethod) && m.getParameterCount() == 1) {
                                method = m;
                                break;
                            }
                        }
                        if (method != null) {
                            method.setAccessible(true);
                            invokeSetter(method, jdbcConfig, value);
                        } else {
                            throw new IllegalArgumentException(
                                "jdbcConfig error,property:"
                                    + key
                                    + " invalid,please see more properties jdbcConfig https://github.com/brettwooldridge/HikariCP");
                        }
                    }
                }
                ds = new HikariDataSource(jdbcConfig);
                DATA_SOURCE_HOLDER.put(alias, ds);
            }
            return ds.getConnection();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to obtain JDBC connection for alias: " + alias, e);
        } finally {
            lock.unlock();
        }
    }

    private static void setFieldValue(Field field, HikariConfig jdbcConfig,
                                      String value) throws IllegalAccessException {
        String type = field.getType().getSimpleName();
        switch (type) {
            case "String":
                field.set(jdbcConfig, value);
                break;
            case "int":
                field.setInt(jdbcConfig, Integer.parseInt(value));
                break;
            case "long":
                field.setLong(jdbcConfig, Long.parseLong(value));
                break;
            case "boolean":
                field.setBoolean(jdbcConfig, Boolean.parseBoolean(value));
                break;
            default:
                break;
        }
    }

    private static void invokeSetter(Method method, HikariConfig jdbcConfig, String value) throws Exception {
        String type = method.getParameterTypes()[0].getSimpleName();
        switch (type) {
            case "String":
                method.invoke(jdbcConfig, value);
                break;
            case "int":
            case "Integer":
                method.invoke(jdbcConfig, Integer.parseInt(value));
                break;
            case "long":
            case "Long":
                method.invoke(jdbcConfig, Long.parseLong(value));
                break;
            case "boolean":
            case "Boolean":
                method.invoke(jdbcConfig, Boolean.parseBoolean(value));
                break;
            default:
                break;
        }
    }

    private static Statement createStatement(Connection conn) throws Exception {
        return conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    public static void close(AutoCloseable... closeable) {
        for (AutoCloseable item : closeable) {
            if (item != null) {
                try {
                    item.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }
}
