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

import org.apache.streampark.catalog.connections.JdbcConnectionOptions;
import org.apache.streampark.catalog.connections.JdbcConnectionProvider;
import org.apache.streampark.catalog.connections.SimpleJdbcConnectionProvider;
import org.apache.streampark.catalog.mysql.MysqlBaseITCASE;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.catalog.CatalogStore;
import org.apache.flink.table.catalog.CommonCatalogOptions;
import org.apache.flink.table.catalog.GenericInMemoryCatalogFactoryOptions;
import org.apache.flink.table.catalog.exceptions.CatalogException;

import org.assertj.core.api.ThrowableAssert;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class JdbcCatalogStoreTest extends MysqlBaseITCASE {

    private static final String DUMMY = "dummy";
    private static final CatalogDescriptor DUMMY_CATALOG;

    static {
        Configuration conf = new Configuration();
        conf.set(CommonCatalogOptions.CATALOG_TYPE, DUMMY);
        conf.set(GenericInMemoryCatalogFactoryOptions.DEFAULT_DATABASE, "dummy_db");

        DUMMY_CATALOG = CatalogDescriptor.of(DUMMY, conf);
    }
    @org.junit.Test
    public void testNotOpened() {
        CatalogStore catalogStore = initCatalogStore();

        assertCatalogStoreNotOpened(catalogStore::listCatalogs);
        assertCatalogStoreNotOpened(() -> catalogStore.contains(DUMMY));
        assertCatalogStoreNotOpened(() -> catalogStore.getCatalog(DUMMY));
        assertCatalogStoreNotOpened(() -> catalogStore.storeCatalog(DUMMY, DUMMY_CATALOG));
        assertCatalogStoreNotOpened(() -> catalogStore.removeCatalog(DUMMY, true));
    }

    @org.junit.Test
    public void testStore() {
        CatalogStore catalogStore = initCatalogStore();
        catalogStore.open();

        catalogStore.storeCatalog(DUMMY, DUMMY_CATALOG);

        Set<String> storedCatalogs = catalogStore.listCatalogs();
        assertThat(storedCatalogs.size()).isEqualTo(1);
        assertThat(storedCatalogs.contains(DUMMY)).isTrue();
    }

    @org.junit.Test
    public void testRemoveExisting() {
        CatalogStore catalogStore = initCatalogStore();
        catalogStore.open();

        catalogStore.storeCatalog(DUMMY, DUMMY_CATALOG);
        assertThat(catalogStore.listCatalogs().size()).isEqualTo(1);

        catalogStore.removeCatalog(DUMMY, false);
        assertThat(catalogStore.listCatalogs().size()).isEqualTo(0);
        assertThat(catalogStore.contains(DUMMY)).isFalse();
    }

    @org.junit.Test
    public void testRemoveNonExisting() {
        CatalogStore catalogStore = initCatalogStore();
        catalogStore.open();

        catalogStore.removeCatalog(DUMMY, true);

        assertThatThrownBy(() -> catalogStore.removeCatalog(DUMMY, false))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining(
                "Remove catalog " + DUMMY + " failed!");
    }

    @org.junit.Test
    public void testClosed() {
        CatalogStore catalogStore = initCatalogStore();

        catalogStore.open();

        catalogStore.storeCatalog(DUMMY, DUMMY_CATALOG);
        assertThat(catalogStore.listCatalogs().size()).isEqualTo(1);

        catalogStore.close();

        assertCatalogStoreNotOpened(catalogStore::listCatalogs);
        assertCatalogStoreNotOpened(() -> catalogStore.contains(DUMMY));
        assertCatalogStoreNotOpened(() -> catalogStore.getCatalog(DUMMY));
        assertCatalogStoreNotOpened(() -> catalogStore.storeCatalog(DUMMY, DUMMY_CATALOG));
        assertCatalogStoreNotOpened(() -> catalogStore.removeCatalog(DUMMY, true));
    }

    private void assertCatalogStoreNotOpened(
                                             ThrowableAssert.ThrowingCallable shouldRaiseThrowable) {
        assertThatThrownBy(shouldRaiseThrowable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CatalogStore is not opened yet.");
    }

    private JdbcCatalogStore initCatalogStore() {
        JdbcConnectionOptions jdbcConnectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
            .withUrl(MYSQL_CONTAINER.getJdbcUrl())
            .withDriverName(MYSQL_CONTAINER.getDriverClassName())
            .withUsername(MYSQL_CONTAINER.getUsername())
            .withPassword(MYSQL_CONTAINER.getPassword())
            .build();

        JdbcConnectionProvider jdbcConnectionProvider = new SimpleJdbcConnectionProvider(jdbcConnectionOptions);
        return new JdbcCatalogStore(jdbcConnectionProvider, "t_mysql_catalog");

    }
}
