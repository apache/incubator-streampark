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

package org.apache.streampark.console.base.config;

import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.console.base.util.WebUtils;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class SpringProperties {

  private static final Logger log = LoggerFactory.getLogger(SpringProperties.class);

  public static Properties get() {
    File oldConfig = getOldConfig();
    if (oldConfig != null) {
      log.warn(
          "in the \"conf\" directory, found the \"application.yml\" file. The \"application.yml\" file is deprecated. "
              + "For compatibility, this \"application.yml\" file will be used preferentially. The latest configuration file is \"config.yaml\". "
              + "It is recommended to use \"config.yaml\". Note: \"application.yml\" will be completely deprecated in version 2.2.0. ");
      SystemPropertyUtils.set("spring.config.location", oldConfig.getAbsolutePath());
      return new Properties();
    } else {
      // 1) get spring config
      Properties springConfig = getSpringConfig();
      // 2) get user config
      Properties userConfig = getUserConfig();
      // 3) merge config
      mergeConfig(userConfig, springConfig);
      // 4) datasource
      dataSourceConfig(userConfig, springConfig);
      // 5) system.setProperties
      springConfig.forEach((k, v) -> SystemPropertyUtils.set(k.toString(), v.toString()));
      return springConfig;
    }
  }

  private static void dataSourceConfig(Properties userConfig, Properties springConfig) {
    String dialect = userConfig.getProperty("datasource.dialect", "").toString();
    if (StringUtils.isBlank(dialect)) {
      throw new ExceptionInInitializerError(
          "datasource.dialect is required, please check config.yaml");
    }
    switch (dialect.toLowerCase()) {
      case "mysql":
        try {
          Class.forName("com.mysql.cj.jdbc.Driver");
          springConfig.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
          try {
            Class.forName("com.mysql.jdbc.Driver");
            springConfig.put("spring.datasource.driver-class-name", "com.mysql.jdbc.Driver");
          } catch (ClassNotFoundException e1) {
            throw new ExceptionInInitializerError(
                "datasource.dialect is mysql, \"com.mysql.cj.jdbc.Driver\" and \"com.mysql.jdbc.Driver\" classes not found, Please ensure that the MySQL Connector/J can be found under $streampark/lib,\n"
                    + "Notice: The MySQL Connector/J is incompatible with the Apache 2.0 license, You need to download and put it into $streampark/lib");
          }
        }
        break;
      case "postgresql":
      case "pgsql":
        springConfig.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        break;
      case "h2":
        springConfig.put("spring.datasource.driver-class-name", "org.h2.Driver");
        springConfig.put("spring.datasource.username", "sa");
        springConfig.put("spring.datasource.password", "sa");
        springConfig.put(
            "spring.datasource.url",
            "jdbc:h2:mem:streampark;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true;INIT=runscript from 'classpath:db/schema-h2.sql'");
        springConfig.put("spring.sql.init.data-locations", "classpath:db/data-h2.sql");
        springConfig.put("spring.sql.init.continue-on-error", "true");
        springConfig.put("spring.sql.init.username", "sa");
        springConfig.put("spring.sql.init.password", "sa");
        springConfig.put("spring.sql.init.mode", "always");
        break;
      default:
        throw new UnsupportedOperationException("Unsupported datasource dialect: " + dialect);
    }
  }

  private static void mergeConfig(Properties userConfig, Properties springConfig) {

    Map<String, String> configMapping = Maps.newHashMap();
    configMapping.put("datasource.username", "spring.datasource.username");
    configMapping.put("datasource.password", "spring.datasource.password");
    configMapping.put("datasource.url", "spring.datasource.url");

    userConfig.forEach(
        (k, v) -> {
          String key = configMapping.get(k);
          if (key != null) {
            springConfig.put(key, v);
          } else {
            springConfig.put(k, v);
          }
        });
  }

  private static boolean useOldConfig() {
    String appHome = WebUtils.getAppHome();
    if (appHome == null) {
      return false;
    }
    File file = new File(appHome + "/conf/application.yml");
    return file.exists();
  }

  private static Properties getUserConfig() {
    String appHome = WebUtils.getAppHome();
    Properties properties = new Properties();
    if (appHome != null) {
      File file = new File(appHome + "/conf/config.yaml");
      if (file.exists() && file.isFile()) {
        Map<String, String> config = PropertiesUtils.fromYamlFileAsJava(file.getAbsolutePath());
        properties.putAll(config);
        return properties;
      }
      throw new ExceptionInInitializerError(file.getAbsolutePath() + " not found, please check.");
    } else {
      InputStream inputStream =
          SpringProperties.class.getClassLoader().getResourceAsStream("config.yaml");
      Map<String, String> config = PropertiesUtils.fromYamlFileAsJava(inputStream);
      properties.putAll(config);
      return properties;
    }
  }

  private static Properties getSpringConfig() {
    Properties config = new Properties();
    // basic
    config.put("spring.application.name", "Apache StreamPark");
    config.put("spring.main.banner-mode", "false");
    config.put("spring.devtools.restart.enabled", "false");
    config.put("spring.aop.proxy-target-class", "true");
    config.put("spring.messages.encoding", "utf-8");
    config.put("spring.main.allow-circular-references", "true");
    config.put("spring.mvc.converters.preferred-json-mapper", "jackson");

    // jackson
    config.put("spring.jackson.date-format", "yyyy-MM-dd HH:mm:ss");
    config.put("spring.jackson.time-zone", "GMT+8");
    config.put("spring.jackson.deserialization.fail-on-unknown-properties", "false");

    // multipart
    config.put("spring.servlet.multipart.enabled", "true");
    config.put("spring.servlet.multipart.max-file-size", "-1");
    config.put("spring.servlet.multipart.max-request-size", "-1");

    // swagger-ui
    config.put("knife4j.enable", "false");
    config.put("knife4j.basic.enable", "false");
    config.put("springdoc.api-docs.enabled", "false");
    config.put("knife4j.basic.username", "admin");
    config.put("knife4j.basic.password", "streampark");
    config.put("springdoc.swagger-ui.path", "/swagger-ui.html");
    config.put("springdoc.packages-to-scan", "org.apache.streampark.console");
    config.put("spring.mvc.pathmatch.matching-strategy", "ant_path_matcher");

    // metrics
    config.put("management.health.ldap.enabled", "false");
    return config;
  }

  private static File getOldConfig() {
    String appHome = WebUtils.getAppHome();
    if (appHome == null) {
      return null;
    }
    File file = new File(appHome + "/conf/application.yml");
    if (file.exists()) {
      return file;
    }
    return null;
  }
}
