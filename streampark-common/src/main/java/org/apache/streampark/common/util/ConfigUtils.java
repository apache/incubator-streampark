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
import org.apache.streampark.common.constants.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ConfigUtils {

    private ConfigUtils() {
    }

    public static Properties getConf(Map<String, String> parameter) {
        return getConf(parameter, "", "", "");
    }

    public static Properties getConf(Map<String, String> parameter, String prefix) {
        return getConf(parameter, prefix, "", "");
    }

    public static Properties getConf(
                                     Map<String, String> parameter, String prefix, String addfix, String alias) {
        Map<String, String> map = filterParam(parameter, prefix + alias);
        Properties prop = new Properties();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                prop.put(addfix + entry.getKey(), entry.getValue());
            }
        }
        return prop;
    }

    public static Properties getHBaseConfig(Map<String, String> parameter) {
        return getConf(parameter, ConfigKeys.HBASE_PREFIX(), ConfigKeys.HBASE_PREFIX(), "");
    }

    public static Properties getInfluxConfig(Map<String, String> parameter) {
        return getConf(parameter, ConfigKeys.INFLUX_PREFIX(), "", "");
    }

    public static Properties getKafkaSinkConf(Map<String, String> parameter, String topic) {
        return getKafkaSinkConf(parameter, topic, "");
    }

    public static Properties getKafkaSinkConf(
                                              Map<String, String> parameter, String topic, String alias) {
        String prefix = ConfigKeys.KAFKA_SINK_PREFIX() + alias;
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        Map<String, String> param = filterParam(parameter, prefix);
        if (param.isEmpty()) {
            throw new IllegalArgumentException(topic + " init error...");
        }
        Properties kafkaProperty = new Properties();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            kafkaProperty.put(entry.getKey(), entry.getValue().trim());
        }
        String resolvedTopic = topic;
        if (Constants.EMPTY_STRING.equals(topic)) {
            Object top = kafkaProperty.get(ConfigKeys.KEY_KAFKA_TOPIC());
            if (top == null || top.toString().split(",|\\s+").length > 1) {
                throw new IllegalArgumentException(
                    "Can't find a unique topic!!!,you must be input a topic");
            }
            resolvedTopic = top.toString();
        }
        boolean hasTopic = true;
        for (Map.Entry<Object, Object> entry : kafkaProperty.entrySet()) {
            if (ConfigKeys.KEY_KAFKA_TOPIC().equals(entry.getKey())) {
                for (String t : entry.getValue().toString().split(",|\\s+")) {
                    if (t.equals(resolvedTopic)) {
                        hasTopic = false;
                        break;
                    }
                }
            }
        }
        if (hasTopic) {
            throw new IllegalArgumentException("Can't find a topic of:" + resolvedTopic + "!!!");
        }
        kafkaProperty.put(ConfigKeys.KEY_KAFKA_TOPIC(), resolvedTopic);
        return kafkaProperty;
    }

    public static Properties getJdbcConf(Map<String, String> parameter) {
        return getJdbcConf(parameter, "");
    }

    public static Properties getJdbcConf(Map<String, String> parameter, String alias) {
        String prefix;
        if (alias == null || alias.isEmpty()) {
            prefix = ConfigKeys.KEY_JDBC_PREFIX();
        } else {
            prefix = ConfigKeys.KEY_JDBC_PREFIX() + alias;
            if (!prefix.endsWith(".")) {
                prefix = prefix + ".";
            }
        }
        String driver = parameter.getOrDefault(prefix + ConfigKeys.KEY_JDBC_DRIVER(), null);
        String url = parameter.getOrDefault(prefix + ConfigKeys.KEY_JDBC_URL(), null);
        String user = parameter.getOrDefault(prefix + ConfigKeys.KEY_JDBC_USER(), null);
        String password = parameter.getOrDefault(prefix + ConfigKeys.KEY_JDBC_PASSWORD(), null);
        if (driver == null || url == null) {
            throw new IllegalArgumentException(
                "Jdbc instance:" + prefix + " error,[driver|url] must not be null");
        }
        if ((user != null && password == null) || (user == null && password != null)) {
            throw new IllegalArgumentException(
                "Jdbc instance:"
                    + prefix
                    + " error, [user|password] must be all null,or all not null ");
        }
        Map<String, String> param = filterParam(parameter, prefix);
        Properties properties = new Properties();
        String aliasName = alias == null || alias.trim().isEmpty() ? "default" : alias;
        properties.put(ConfigKeys.KEY_ALIAS(), aliasName);
        properties.put(ConfigKeys.KEY_JDBC_DRIVER(), driver);
        for (Map.Entry<String, String> entry : param.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }
        return properties;
    }

    public static Properties getConf(Properties properties, String prefix, String addfix) {
        return getConfFromProperties(properties, prefix, addfix, "");
    }

    public static Properties getConfFromProperties(
                                                   Properties properties, String prefix, String addfix, String alias) {
        Map<String, String> map = new HashMap<>();
        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    map.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
        return getConf(map, prefix, addfix, alias);
    }

    public static Map<String, String> getConfMap(Properties properties, String prefix) {
        Properties props = getConfFromProperties(properties, prefix, "", "");
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }

    private static Map<String, String> filterParam(Map<String, String> parameter, String fix) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : parameter.entrySet()) {
            if (entry.getKey().startsWith(fix) && entry.getValue() != null) {
                result.put(
                    entry.getKey().substring(fix.length()).replaceFirst("^\\.", ""),
                    entry.getValue());
            }
        }
        return result;
    }
}
