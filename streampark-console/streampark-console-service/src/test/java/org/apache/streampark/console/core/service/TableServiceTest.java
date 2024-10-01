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
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.bean.FlinkCatalogParams;
import org.apache.streampark.console.core.bean.FlinkDataType;
import org.apache.streampark.console.core.bean.TableColumn;
import org.apache.streampark.console.core.bean.TableParams;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.service.container.MysqlBaseITCASE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableServiceTest extends MysqlBaseITCASE {

    @Autowired
    private FlinkCatalogService catalogService;
    @Autowired
    private TableService tableService;
    private FlinkCatalogParams flinkCatalog = new FlinkCatalogParams();
    private final String FLINK_TABLE = "flink_test";
    private final String FLINK_NEW_TABLE = "flink_test_new";

    @BeforeEach
    public void setUp() {
        flinkCatalog.setCatalogName("flink-test");
        flinkCatalog.setTeamId(1L);
        flinkCatalog.setUserId(1L);
        flinkCatalog.setCatalogType(CatalogType.JDBC);
        FlinkCatalogParams.FlinkJDBCCatalog flinkJDBCCatalog =
            new FlinkCatalogParams.FlinkJDBCCatalog();
        flinkJDBCCatalog.setBaseUrl(MYSQL_CONTAINER.getJdbcUrl());
        flinkJDBCCatalog.setType(CatalogType.JDBC.name().toLowerCase());
        flinkJDBCCatalog.setDefaultDatabase(MYSQL_CONTAINER.getDatabaseName());
        flinkJDBCCatalog.setUsername(MYSQL_CONTAINER.getUsername());
        flinkJDBCCatalog.setPassword(MYSQL_CONTAINER.getPassword());
        flinkCatalog.setFlinkJDBCCatalog(flinkJDBCCatalog);
        catalogService.create(flinkCatalog, 1L);
        FlinkCatalog fc = catalogService.getCatalog("flink-test");
        flinkCatalog.setId(fc.getId());
    }

    @AfterEach
    void destroy() {
        FlinkCatalog fc = catalogService.getCatalog("flink-test");
        catalogService.remove(flinkCatalog.getId());
    }

    @Test
    @Order(1)
    public void testTableExists_Positive() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);

        assertTrue(tableService.tableExists(tableParams));
    }

    @Test
    @Order(2)
    public void testTableExists_Negative_CatalogNotFound() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(-100L);
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);

        Exception exception =
            assertThrows(ApiAlertException.class, () -> tableService.tableExists(tableParams));
        assertEquals("Catalog is not exit.", exception.getMessage());
    }

    @Test
    @Order(3)
    public void testCreateTable_Positive() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);
        tableParams.setDescription("Test table");
        List<String> partitionKeyList = new ArrayList();
        partitionKeyList.add("A");
        tableParams.setPartitionKey(partitionKeyList);
        Map<String, String> optionMap = new HashMap<>();
        optionMap.put("A", "A");
        tableParams.setTableOptions(optionMap);
        FlinkDataType flinkDataType = new FlinkDataType("INT", true, 8, 16);
        tableParams.setTableColumns(
            Collections.singletonList(
                new TableColumn(1, "A", flinkDataType, "table test", true, "1", 1)));

        Exception exception =
            assertThrows(
                UnsupportedOperationException.class, () -> tableService.createTable(tableParams));
        assertEquals(null, exception.getMessage());
    }

    @Test
    @Order(4)
    public void testCreateTable_Negative_TableNameNull() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(null);
        tableParams.setDescription("Test table");
        FlinkDataType flinkDataType = new FlinkDataType("INT", true, 8, 16);
        tableParams.setTableColumns(
            Collections.singletonList(
                new TableColumn(1, "A", flinkDataType, "table test", false, "1", 1)));

        Exception exception =
            assertThrows(ApiAlertException.class, () -> tableService.createTable(tableParams));
        assertEquals("Table name can not be null.", exception.getMessage());
    }

    @Test
    @Order(5)
    public void testAddColumn_Positive() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);
        tableParams.setDescription("Test table");
        FlinkDataType flinkDataType = new FlinkDataType("INT", true, 8, 16);
        tableParams.setTableColumns(
            Collections.singletonList(
                new TableColumn(1, "B", flinkDataType, "table test", false, "1", 1)));

        Exception exception =
            assertThrows(
                UnsupportedOperationException.class, () -> tableService.addColumn(tableParams));
        assertEquals(null, exception.getMessage());
    }

    @Test
    @Order(6)
    public void testAddColumn_Negative_TableNameNull() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(null);
        tableParams.setDescription("Test table");
        FlinkDataType flinkDataType = new FlinkDataType("INT", true, 8, 16);
        tableParams.setTableColumns(
            Collections.singletonList(
                new TableColumn(1, "A", flinkDataType, "table test", false, "1", 1)));

        Exception exception =
            assertThrows(ApiAlertException.class, () -> tableService.addColumn(tableParams));
        assertEquals("Table name can not be null.", exception.getMessage());
    }

    @Test
    @Order(7)
    public void testDropColumn() {
        String catalogName = flinkCatalog.getCatalogName();
        String databaseName = MYSQL_CONTAINER.getDatabaseName();
        String tableName = FLINK_TABLE;
        String columnName = "A";
        Exception exception =
            assertThrows(
                UnsupportedOperationException.class,
                () -> tableService.dropColumn(catalogName, databaseName, tableName, columnName));
        assertEquals(null, exception.getMessage());
    }

    @Test
    @Order(8)
    public void testDropColumn_Negative_TableNameNull() {
        String catalogName = "flink-test";
        String databaseName = "test_db";
        String columnName = "old_column";

        Exception exception =
            assertThrows(
                ApiAlertException.class,
                () -> tableService.dropColumn(catalogName, databaseName, null, columnName));
        assertEquals("Table name can not be null.", exception.getMessage());
    }

    @Test
    @Order(9)
    public void testAddOption() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);
        tableParams.setTableOptions(Collections.singletonMap("key", "value"));

        Exception exception =
            assertThrows(
                UnsupportedOperationException.class, () -> tableService.addOption(tableParams));
        assertEquals(
            "Unsupported table change type: org.apache.flink.table.catalog.TableChange$SetOption",
            exception.getMessage());
    }

    @Test
    @Order(10)
    public void testAddOption_Negative_OptionsNull() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);

        Exception exception =
            assertThrows(ApiAlertException.class, () -> tableService.addOption(tableParams));
        assertEquals("Table options can not be null.", exception.getMessage());
    }

    @Test
    @Order(12)
    public void testDropTable_Negative_TableNameNull() {
        String catalogName = "test_catalog";
        String databaseName = "test_db";

        Exception exception =
            assertThrows(
                ApiAlertException.class, () -> tableService.dropTable(catalogName, databaseName, null));
        assertEquals("Table name can not be null.", exception.getMessage());
    }

    @Test
    @Order(13)
    public void testRenameTable() {
        String catalogName = flinkCatalog.getCatalogName();
        String databaseName = MYSQL_CONTAINER.getDatabaseName();
        Exception exception =
            assertThrows(
                UnsupportedOperationException.class,
                () -> tableService.renameTable(catalogName, databaseName, FLINK_TABLE, FLINK_NEW_TABLE));
        assertEquals(null, exception.getMessage());
    }

    @Test
    @Order(14)
    public void testRenameTable_Negative_FromTableNameNull() {
        String catalogName = "test_catalog";
        String databaseName = "test_db";
        String toTableName = "new_table";

        Exception exception =
            assertThrows(
                ApiAlertException.class,
                () -> tableService.renameTable(catalogName, databaseName, null, toTableName));
        assertEquals("From table name can not be null.", exception.getMessage());
    }

    @Test
    @Order(15)
    public void testListTables_Positive() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setCatalogName(flinkCatalog.getCatalogName());
        tableParams.setDatabaseName(MYSQL_CONTAINER.getDatabaseName());
        tableParams.setName(FLINK_TABLE);

        List<TableParams> result = tableService.listTables(tableParams);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @Order(16)
    public void testListTables_Negative_DatabaseNameNull() {
        TableParams tableParams = new TableParams();
        tableParams.setCatalogId(flinkCatalog.getId());
        tableParams.setDatabaseName(null);
        tableParams.setName("test_table");

        Exception exception =
            assertThrows(ApiAlertException.class, () -> tableService.listTables(tableParams));
        assertEquals("Database name can not be null.", exception.getMessage());
    }

    @Test
    @Order(17)
    public void testListColumns_Positive() {
        String catalogName = flinkCatalog.getCatalogName();
        String databaseName = MYSQL_CONTAINER.getDatabaseName();
        TableParams result = tableService.listColumns(catalogName, databaseName, FLINK_TABLE);
        assertNotNull(result);
    }

    @Test
    @Order(17)
    public void testListColumns_Negative_DatabaseNameNull() {
        String catalogName = "test_catalog";
        String tableName = "test_table";

        Exception exception =
            assertThrows(
                ApiAlertException.class, () -> tableService.listColumns(catalogName, null, tableName));
        assertEquals("Database name can not be null.", exception.getMessage());
    }
}
