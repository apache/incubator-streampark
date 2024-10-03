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

package org.apache.streampark.console.core.bean;

import org.apache.streampark.common.enums.CatalogType;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.FlinkCatalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Slf4j
public class FlinkCatalogParams implements Serializable {

    private Long id;

    private Long teamId;

    private String catalogName;

    private CatalogType catalogType;

    /** creator */
    private Long userId;

    private Date createTime;

    private Date updateTime;

    private FlinkJDBCCatalog flinkJDBCCatalog;

    private FlinkHiveCatalog flinkHiveCatalog;

    private FlinkPaimonCatalog flinkPaimonCatalog;

    private String customCatalogConfig;

    public static FlinkCatalogParams of(FlinkCatalog flinkCatalog) {
        if (flinkCatalog == null) {
            return null;
        }
        FlinkCatalogParams flinkCatalogParams = new FlinkCatalogParams();
        BeanUtils.copyProperties(flinkCatalog, flinkCatalogParams, "configuration");
        try {
            switch (flinkCatalog.getCatalogType()) {
                case MYSQL:
                case PGSQL:
                case ORACLE:
                case JDBC:
                    flinkCatalogParams.setFlinkJDBCCatalog(
                        JacksonUtils.read(flinkCatalog.getConfiguration(), FlinkJDBCCatalog.class));
                    break;
                case HIVE:
                    flinkCatalogParams.setFlinkHiveCatalog(
                        JacksonUtils.read(flinkCatalog.getConfiguration(), FlinkHiveCatalog.class));
                    break;
                case PAIMON:
                    flinkCatalogParams.setFlinkPaimonCatalog(
                        JacksonUtils.read(flinkCatalog.getConfiguration(), FlinkPaimonCatalog.class));
                    break;
                case CUSTOM:
                    flinkCatalogParams.setCustomCatalogConfig(flinkCatalog.getConfiguration());
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error("Flink catalog params json read failed", e);
            throw new RuntimeException("Flink catalog params json read failed");
        }

        return flinkCatalogParams;
    }

    @Getter
    @Setter
    public static class FlinkJDBCCatalog implements Serializable {

        @NotBlank
        private String type;

        @NotBlank
        @JsonProperty("default-database")
        private String defaultDatabase;

        @NotBlank
        private String username;
        @NotBlank
        private String password;

        @NotBlank
        @JsonProperty("base-url")
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class FlinkHiveCatalog implements Serializable {

        @NotBlank
        private String type;

        @JsonProperty("hive-conf-dir")
        private String hiveConfDir;

        @JsonProperty("default-database")
        private String defaultDatabase;

        @JsonProperty("hive-version")
        private String hiveVersion;

        @JsonProperty("hadoop-conf-dir")
        private String hadoopConfDir;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHiveConfDir() {
            return hiveConfDir;
        }

        public void setHiveConfDir(String hiveConfDir) {
            this.hiveConfDir = hiveConfDir;
        }

        public String getDefaultDatabase() {
            return defaultDatabase;
        }

        public void setDefaultDatabase(String defaultDatabase) {
            this.defaultDatabase = defaultDatabase;
        }

        public String getHiveVersion() {
            return hiveVersion;
        }

        public void setHiveVersion(String hiveVersion) {
            this.hiveVersion = hiveVersion;
        }

        public String getHadoopConfDir() {
            return hadoopConfDir;
        }

        public void setHadoopConfDir(String hadoopConfDir) {
            this.hadoopConfDir = hadoopConfDir;
        }
    }

    @Getter
    @Setter
    public static class FlinkPaimonCatalog implements Serializable {

        @NotBlank
        private String type;
        @NotBlank
        private String warehouse;
        @NotBlank
        private String metastore; // hive filesystem
        private String uri;

        @JsonProperty("hive-conf-dir")
        private String hiveConfDir;

        @JsonProperty("hadoop-conf-dir")
        private String hadoopConfDir;
    }
}
