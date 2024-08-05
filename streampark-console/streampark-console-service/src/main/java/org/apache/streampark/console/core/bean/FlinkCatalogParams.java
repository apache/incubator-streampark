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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.beans.BeanUtils;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

@Data
@Slf4j
public class FlinkCatalogParams implements Serializable {

    private Long id;

    private Long teamId;

    private String catalogName;

    private CatalogType catalogType;

    /** creator */
    private Long userId;

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
            throw new RuntimeException(e);
        }

        return flinkCatalogParams;
    }

    @Data
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

    @Data
    public static class FlinkHiveCatalog implements Serializable {

        @NotBlank
        private String type;
        @NotBlank
        private String name;

        @JsonProperty("hive-conf-dir")
        private String hiveConfDir;

        @JsonProperty("default-database")
        private String defaultDatabase;

        @JsonProperty("hive-version")
        private String hiveVersion;

        @JsonProperty("hadoop-conf-dir")
        private String hadoopConfDir;
    }

    @Data
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
