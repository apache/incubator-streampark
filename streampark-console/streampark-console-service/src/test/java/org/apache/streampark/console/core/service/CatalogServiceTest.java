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

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.core.entity.Catalog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CatalogService Tests */
public class CatalogServiceTest extends SpringUnitTestBase {

    @Autowired
    private CatalogService catalogService;

    @AfterEach
    void cleanTestRecordsInDatabase() {
        catalogService.remove(new QueryWrapper<>());
    }

    @Test
    public void create() {
        Catalog catalog = new Catalog();
        catalog.setTeamId(1L);
        catalog.setCatalogName("catalog-name");
        catalog.setConfiguration("{\"test\": \" test\"}");
        boolean create = catalogService.create(catalog, 1L);
        assertThat(create).isTrue();
    }

    @Test
    public void remove() {
        Catalog catalog = new Catalog();
        // catalog.setId(2L);
        catalog.setTeamId(1L);
        catalog.setCatalogName("catalog-name");
        catalog.setConfiguration("{\"test\": \" test\"}");
        catalogService.create(catalog, 1L);
        RestRequest request = new RestRequest();
        IPage<Catalog> catalogIPage = catalogService.page(catalog, request);
        boolean deleted = catalogService.remove(catalog.getId());
        assertThat(deleted).isTrue();
    }

    @Test
    public void update() {
        Catalog catalog = new Catalog();
        // catalog.setId(2L);
        catalog.setTeamId(1L);
        catalog.setCatalogName("catalog-name");
        catalog.setConfiguration("{\"test\": \" test\"}");
        catalogService.create(catalog, 1L);

        catalog.setConfiguration("{\"test1\": \" test1\"}");
        RestRequest request = new RestRequest();
        IPage<Catalog> catalogIPage = catalogService.page(catalog, request);
        List<Catalog> catalogs = catalogIPage.getRecords();

        assertThat(catalogs.get(0).getConfiguration().contains("test1")).isTrue();
    }
}
