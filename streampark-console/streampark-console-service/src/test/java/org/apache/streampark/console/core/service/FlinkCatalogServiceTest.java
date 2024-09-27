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

package org.apache.streampark.console.core.service;

import org.apache.streampark.common.enums.CatalogType;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.bean.FlinkCatalogParams;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** CatalogService Tests */
public class FlinkCatalogServiceTest extends SpringUnitTestBase {

    @Autowired
    private FlinkCatalogService catalogService;

    @AfterEach
    void cleanTestRecordsInDatabase() {
        catalogService.remove(new QueryWrapper<>());
    }

    @Test
    @Order(1)
    public void create() {
        FlinkCatalogParams catalog = new FlinkCatalogParams();
        catalog.setTeamId(1L);
        catalog.setCatalogType(CatalogType.JDBC);
        catalog.setCatalogName("catalog-name");
        FlinkCatalogParams.FlinkJDBCCatalog flinkJDBCCatalog =
            new FlinkCatalogParams.FlinkJDBCCatalog();
        flinkJDBCCatalog.setType("jdbc");
        flinkJDBCCatalog.setDefaultDatabase("aa");
        flinkJDBCCatalog.setPassword("11");
        flinkJDBCCatalog.setUsername("user");
        flinkJDBCCatalog.setBaseUrl("url");
        catalog.setFlinkJDBCCatalog(flinkJDBCCatalog);

        boolean create = catalogService.create(catalog, 1L);
        assertThat(create).isTrue();
    }

    @Test
    @Order(2)
    public void update() {
        FlinkCatalogParams catalog = new FlinkCatalogParams();
        catalog.setTeamId(1L);
        catalog.setCatalogType(CatalogType.JDBC);
        catalog.setCatalogName("catalog-name");
        FlinkCatalogParams.FlinkJDBCCatalog flinkJDBCCatalog =
            new FlinkCatalogParams.FlinkJDBCCatalog();
        flinkJDBCCatalog.setType("jdbc");
        flinkJDBCCatalog.setDefaultDatabase("aa");
        flinkJDBCCatalog.setPassword("11");
        flinkJDBCCatalog.setUsername("user");
        flinkJDBCCatalog.setBaseUrl("url1");
        catalog.setFlinkJDBCCatalog(flinkJDBCCatalog);
        RestRequest request = new RestRequest();
        catalogService.create(catalog, 1L);

        IPage<FlinkCatalogParams> catalogIPage = catalogService.page(catalog, request);
        FlinkCatalogParams catalogs = catalogIPage.getRecords().get(0);
        catalogs.getFlinkJDBCCatalog().setBaseUrl("url2");
        catalogService.update(catalogs, 2L);

        IPage<FlinkCatalogParams> catalogResult = catalogService.page(catalog, request);

        assertThat(
            catalogResult.getRecords().get(0).getFlinkJDBCCatalog().getBaseUrl().contains("url2"))
                .isTrue();
        assertThat(catalogResult.getRecords().get(0).getUserId().equals(2L)).isTrue();
        assertThat(catalogResult.getRecords().get(0).getCatalogType().equals(CatalogType.JDBC))
            .isTrue();
    }

    @Test
    @Order(3)
    public void remove() {
        FlinkCatalogParams catalog = new FlinkCatalogParams();
        catalog.setTeamId(1L);
        catalog.setCatalogType(CatalogType.JDBC);
        catalog.setCatalogName("catalog-name");
        FlinkCatalogParams.FlinkJDBCCatalog flinkJDBCCatalog =
            new FlinkCatalogParams.FlinkJDBCCatalog();
        flinkJDBCCatalog.setType("jdbc");
        flinkJDBCCatalog.setDefaultDatabase("aa");
        flinkJDBCCatalog.setPassword("11");
        flinkJDBCCatalog.setUsername("user");
        flinkJDBCCatalog.setBaseUrl("url");
        catalog.setFlinkJDBCCatalog(flinkJDBCCatalog);
        catalogService.create(catalog, 1L);
        RestRequest request = new RestRequest();
        IPage<FlinkCatalogParams> catalogIPage = catalogService.page(catalog, request);
        boolean deleted = catalogService.remove(catalogIPage.getRecords().get(0).getId());
        assertThat(deleted).isTrue();
    }
}
