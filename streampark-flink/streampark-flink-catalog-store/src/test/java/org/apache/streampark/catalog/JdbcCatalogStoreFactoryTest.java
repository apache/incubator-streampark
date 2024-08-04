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

package org.apache.streampark.catalog;

import org.apache.streampark.catalog.mysql.MysqlBaseITCASE;

import org.apache.flink.table.catalog.CatalogStore;
import org.apache.flink.table.factories.CatalogStoreFactory;
import org.apache.flink.table.factories.FactoryUtil;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JdbcCatalogStoreFactoryTest extends MysqlBaseITCASE {

    @org.junit.Test
    public void testCatalogStoreFactoryDiscovery() {

        String factoryIdentifier = JdbcCatalogStoreFactoryOptions.IDENTIFIER;
        Map<String, String> options = new HashMap<>();
        options.put("url", MYSQL_CONTAINER.getJdbcUrl());
        options.put("table-name", "t_mysql_catalog");
        options.put("driver", MYSQL_CONTAINER.getDriverClassName());
        options.put("username", MYSQL_CONTAINER.getUsername());
        options.put("password", MYSQL_CONTAINER.getPassword());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final FactoryUtil.DefaultCatalogStoreContext discoveryContext =
            new FactoryUtil.DefaultCatalogStoreContext(options, null, classLoader);
        final CatalogStoreFactory factory =
            FactoryUtil.discoverFactory(
                classLoader, CatalogStoreFactory.class, factoryIdentifier);
        factory.open(discoveryContext);

        CatalogStore catalogStore = factory.createCatalogStore();
        assertThat(catalogStore instanceof JdbcCatalogStore).isTrue();

        factory.close();
    }
}
