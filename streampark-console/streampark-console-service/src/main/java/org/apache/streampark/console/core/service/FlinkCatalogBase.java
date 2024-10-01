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

import org.apache.streampark.console.core.util.CatalogServiceUtils;

import org.apache.flink.table.api.Schema;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogDatabase;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.CommonCatalogOptions;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.TableChange;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotEmptyException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotExistException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FlinkCatalogBase {

    @Value("${table.catalog-store.kind:jdbc}")
    private String storeKind;

    @Value("${table.catalog-store.jdbc.url:jdbc://mysql:127.0.0.1:3306/flink-test}")
    private String jdbcUrl;

    @Value("${table.catalog-store.jdbc.driver:com.mysql.cj.jdbc.Driver}")
    private String jdbcDriver;

    @Value("${table.catalog-store.jdbc.username:flinkuser}")
    private String jdbcUserName;

    @Value("${table.catalog-store.jdbc.password:flinkpw}")
    private String jdbcPassword;

    @Value("${table.catalog-store.jdbc.max-retry-timeout:600")
    private String jdbcMaxRetryTimeout;

    private final Map<String, Catalog> catalogMap = new ConcurrentHashMap<>(0);

    public List<String> listDatabases(String catalogName, Map<String, String> options) {
        Catalog catalog = getCatalog(catalogName, options);
        return catalog.listDatabases();
    }

    public boolean databaseExists(String catalogName, Map<String, String> options, String database) {
        Catalog catalog = getCatalog(catalogName, options);
        return catalog.databaseExists(database);
    }

    public boolean createDatabase(
                                  String catalogName,
                                  Map<String, String> options,
                                  String databaseName,
                                  CatalogDatabase catalogDatabase,
                                  boolean ignoreIfExists) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            catalog.createDatabase(databaseName, catalogDatabase, ignoreIfExists);
            return true;
        } catch (CatalogException | DatabaseAlreadyExistException e) {
            log.error("create database {} failed.", databaseName, e);
            throw new CatalogException(
                String.format("The database '%s' already exists in the catalog.", databaseName));
        }
    }

    public void dropDatabase(
                             String catalogName,
                             Map<String, String> options,
                             String databaseName,
                             boolean cascade,
                             boolean ignoreIfExists) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            catalog.dropDatabase(databaseName, ignoreIfExists, cascade);
        } catch (CatalogException | DatabaseNotEmptyException | DatabaseNotExistException e) {
            log.error("Drop database {} failed.", databaseName, e);
            throw new CatalogException(
                String.format("The database '%s' already exists in the catalog.", databaseName));
        }
    }

    public boolean tableExists(
                               String catalogName, Map<String, String> options, String databaseName, String tableName) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            return catalog.tableExists(new ObjectPath(databaseName, tableName));
        } catch (CatalogException e) {
            log.error("Table exists {}.{} failed.", databaseName, tableName, e);
            throw new CatalogException(
                String.format("The table  '%s.%s' not exists in the catalog.", databaseName, tableName));
        }
    }

    public boolean createTable(
                               String catalogName,
                               Map<String, String> options,
                               String databaseName,
                               String tableName,
                               CatalogTable catalogTable,
                               boolean ignoreIfExists) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            catalog.createTable(new ObjectPath(databaseName, tableName), catalogTable, ignoreIfExists);
            return true;
        } catch (CatalogException e) {
            log.error("Table {}.{} create failed.", databaseName, tableName, e);
            throw new CatalogException(
                String.format("Table  '%s.%s' create failed.", databaseName, tableName));
        } catch (TableAlreadyExistException | DatabaseNotExistException e) {
            log.error(
                "Table {}.{} create failed.because table is exits or database not exist",
                databaseName,
                tableName,
                e);
            throw new RuntimeException(e);
        }
    }

    public boolean alterTable(
                              String catalogName,
                              Map<String, String> options,
                              String databaseName,
                              String tableName,
                              List<TableChange> changes,
                              boolean ignoreIfExists) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            CatalogBaseTable originTable = getTable(catalogName, options, databaseName, tableName);
            CatalogTable currentTable = (CatalogTable) originTable;

            Schema currentSchema = currentTable.getUnresolvedSchema();
            final Schema schema = applyChangesToSchema(currentSchema, changes);
            Map<String, String> newOptions = new ConcurrentHashMap<>();
            for (TableChange change : changes) {
                if (change instanceof TableChange.SetOption) {
                    newOptions.put(
                        ((TableChange.SetOption) change).getKey(),
                        ((TableChange.SetOption) change).getValue());
                }
                if (change instanceof TableChange.ResetOption) {
                    newOptions.remove(((TableChange.ResetOption) change).getKey());
                }
            }

            final CatalogTable newTable =
                CatalogTable.of(
                    schema, currentTable.getComment(), currentTable.getPartitionKeys(), newOptions);
            catalog.alterTable(new ObjectPath(databaseName, tableName), newTable, ignoreIfExists);
            return true;
        } catch (TableNotExistException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean dropTable(
                             String catalogName,
                             Map<String, String> options,
                             String databaseName,
                             String tableName,
                             boolean ignoreIfExists) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            catalog.dropTable(new ObjectPath(databaseName, tableName), ignoreIfExists);
            return true;
        } catch (TableNotExistException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean renameTable(
                               String catalogName,
                               Map<String, String> options,
                               String databaseName,
                               String fromTableName,
                               String toTableName) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            catalog.renameTable(new ObjectPath(databaseName, fromTableName), toTableName, true);
            return true;
        } catch (TableNotExistException | TableAlreadyExistException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> listTable(
                                  String catalogName, Map<String, String> options, String databaseName) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            return catalog.listTables(databaseName);
        } catch (DatabaseNotExistException e) {
            throw new RuntimeException(e);
        }
    }

    public CatalogBaseTable getTable(
                                     String catalogName, Map<String, String> options, String databaseName,
                                     String tableName) {
        Catalog catalog = getCatalog(catalogName, options);
        try {
            return catalog.getTable(new ObjectPath(databaseName, tableName));
        } catch (TableNotExistException e) {
            throw new RuntimeException(e);
        }
    }

    private Schema applyChangesToSchema(Schema currentSchema, List<TableChange> changes) {
        // Clone the current schema to avoid modifying the original
        Schema.Builder schemaBuilder = Schema.newBuilder().fromSchema(currentSchema);

        // Iterate over each change and apply it to the schema builder
        Set<String> columnsToDrop = new HashSet<>();
        for (TableChange change : changes) {
            if (change instanceof TableChange.AddColumn) {
                TableChange.AddColumn addColumn = (TableChange.AddColumn) change;
                schemaBuilder.column(addColumn.getColumn().getName(), addColumn.getColumn().getDataType());
            } else if (change instanceof TableChange.ModifyColumn) {
                TableChange.ModifyColumn modifyColumn = (TableChange.ModifyColumn) change;
                schemaBuilder.column(
                    modifyColumn.getNewColumn().getName(), modifyColumn.getNewColumn().getDataType());
            } else if (change instanceof TableChange.DropColumn) {
                TableChange.DropColumn removeColumn = (TableChange.DropColumn) change;
                columnsToDrop.add(removeColumn.getColumnName());
            } else {
                throw new UnsupportedOperationException(
                    "Unsupported table change type: " + change.getClass().getName());
            }
        }
        // drop columns
        if (!columnsToDrop.isEmpty()) {
            for (Schema.UnresolvedColumn column : currentSchema.getColumns()) {
                if (column instanceof Schema.UnresolvedPhysicalColumn) {
                    Schema.UnresolvedPhysicalColumn physicalColumn = (Schema.UnresolvedPhysicalColumn) column;
                    if (!columnsToDrop.contains(physicalColumn.getName())) {
                        schemaBuilder.column(physicalColumn.getName(), physicalColumn.getDataType());
                    }
                }
            }
        }
        // Build the updated schema
        return schemaBuilder.build();
    }

    private Catalog getCatalog(String catalogName, Map<String, String> options) {
        if (catalogMap.containsKey(catalogName)) {
            return catalogMap.get(catalogName);
        } else {
            Map<String, String> configuration = new HashMap<>();
            configuration.put(CommonCatalogOptions.TABLE_CATALOG_STORE_KIND.key(), storeKind);
            configuration.put("table.catalog-store.jdbc.url", jdbcUrl);
            configuration.put("table.catalog-store.jdbc.driver", jdbcDriver);
            configuration.put("table.catalog-store.jdbc.username", jdbcUserName);
            configuration.put("table.catalog-store.jdbc.password", jdbcPassword);
            configuration.put("table.catalog-store.jdbc.max-retry-timeout", jdbcMaxRetryTimeout);
            Catalog catalog = CatalogServiceUtils.getCatalog(catalogName, options, configuration);
            catalog.open();
            catalogMap.put(catalogName, catalog);
            return catalog;
        }
    }
}
