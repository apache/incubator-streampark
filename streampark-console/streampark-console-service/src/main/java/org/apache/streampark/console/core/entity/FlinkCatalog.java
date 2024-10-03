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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.enums.CatalogType;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.FlinkCatalogParams;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/** catalog store */
@Getter
@Setter
@TableName("t_flink_catalog")
@Slf4j
public class FlinkCatalog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private String catalogName;

    private CatalogType catalogType;

    private String configuration;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Date createTime;

    private Date updateTime;

    /** creator */
    private Long userId;

    public static FlinkCatalog of(FlinkCatalogParams flinkCatalogParams) {
        if (flinkCatalogParams == null)
            return null;
        FlinkCatalog flinkCatalog = new FlinkCatalog();

        BeanUtils.copyProperties(
            flinkCatalogParams,
            flinkCatalog,
            "flinkJDBCCatalog",
            "flinkHiveCatalog",
            "flinkPaimonCatalog",
            "customCatalogConfig");

        if (null == flinkCatalogParams.getCatalogType()) {
            return flinkCatalog;
        }
        try {
            switch (flinkCatalogParams.getCatalogType()) {
                case MYSQL:
                case PGSQL:
                case ORACLE:
                case JDBC:
                    flinkCatalog.setConfiguration(
                        JacksonUtils.write(flinkCatalogParams.getFlinkJDBCCatalog()));
                    break;
                case HIVE:
                    flinkCatalog.setConfiguration(
                        JacksonUtils.write(flinkCatalogParams.getFlinkHiveCatalog()));
                    break;
                case PAIMON:
                    flinkCatalog.setConfiguration(
                        JacksonUtils.write(flinkCatalogParams.getFlinkPaimonCatalog()));
                    break;
                case CUSTOM:
                    flinkCatalog.setConfiguration(flinkCatalogParams.getCustomCatalogConfig());
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error("Flink catalog json read failed", e);
            throw new RuntimeException("Flink catalog json read failed");
        }
        return flinkCatalog;
    }
}
