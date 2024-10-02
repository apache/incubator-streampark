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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.core.bean.TableColumn;
import org.apache.streampark.console.core.bean.TableParams;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.service.FlinkCatalogBase;
import org.apache.streampark.console.core.service.FlinkCatalogService;
import org.apache.streampark.console.core.service.TableService;
import org.apache.streampark.console.core.util.DataTypeConverterUtils;

import org.apache.flink.table.api.Schema;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.catalog.TableChange;
import org.apache.flink.table.catalog.UniqueConstraint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.streampark.console.core.util.CatalogServiceUtils.getOptions;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class TableServiceImpl implements TableService {

    @Autowired
    private FlinkCatalogService catalogService;
    @Autowired
    private FlinkCatalogBase flinkCatalogBase;

    @Override
    public boolean tableExists(TableParams tableParams) {
        AlertException.throwIfNull(tableParams.getName(), "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(tableParams.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        return flinkCatalogBase.tableExists(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            tableParams.getDatabaseName(),
            tableParams.getName());
    }

    @Override
    public boolean createTable(TableParams tableParams) {
        AlertException.throwIfNull(tableParams.getName(), "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(tableParams.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        AlertException.throwIfNull(tableParams.getTableColumns(), "Table column can not be null.");
        AlertException.throwIfNull(tableParams.getTableOptions(), "Table options can not be null.");
        List<Column> columns = new ArrayList<>();
        List<String> ukColumns = new ArrayList<>();
        AtomicReference<String> ukName = new AtomicReference<>("uk");
        tableParams
            .getTableColumns()
            .forEach(
                tc -> {
                    columns.add(
                        Column.physical(
                            tc.getField(), DataTypeConverterUtils.convertToDataType(tc.getDataType())));
                    if (tc.isPk()) {
                        ukColumns.add(tc.getField());
                        ukName.set(ukName + tc.getField());
                    }
                });
        final Schema schema =
            Schema.newBuilder()
                .fromResolvedSchema(
                    new ResolvedSchema(
                        columns,
                        Collections.emptyList(),
                        UniqueConstraint.primaryKey(ukName.get(), ukColumns)))
                .build();
        final CatalogTable originTable =
            CatalogTable.of(
                schema,
                tableParams.getDescription(),
                tableParams.getPartitionKey(),
                tableParams.getTableOptions());
        return flinkCatalogBase.createTable(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            tableParams.getDatabaseName(),
            tableParams.getName(),
            originTable,
            true);
    }

    @Override
    public boolean addColumn(TableParams tableParams) {
        AlertException.throwIfNull(tableParams.getName(), "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(tableParams.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        AlertException.throwIfNull(tableParams.getTableColumns(), "Table column can not be null.");

        List<TableChange> tableChanges = new ArrayList<>();
        for (TableColumn tableColumn : tableParams.getTableColumns()) {
            Column column =
                Column.physical(
                    tableColumn.getField(),
                    DataTypeConverterUtils.convertToDataType(tableColumn.getDataType()))
                    .withComment(tableColumn.getComment());
            TableChange.AddColumn addColumn = TableChange.add(column);
            tableChanges.add(addColumn);
        }

        return flinkCatalogBase.alterTable(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            tableParams.getDatabaseName(),
            tableParams.getName(),
            tableChanges,
            true);
    }

    @Override
    public boolean dropColumn(
                              String catalogName, String databaseName, String tableName, String columnName) {
        AlertException.throwIfNull(tableName, "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(catalogName);
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        AlertException.throwIfNull(columnName, "Table column name can not be null.");

        List<TableChange> tableChanges = new ArrayList<>();
        TableChange.DropColumn dropColumn = TableChange.dropColumn(columnName);
        tableChanges.add(dropColumn);
        return flinkCatalogBase.alterTable(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            databaseName,
            tableName,
            tableChanges,
            true);
    }

    @Override
    public boolean addOption(TableParams tableParams) {

        AlertException.throwIfNull(tableParams.getName(), "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(tableParams.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        AlertException.throwIfNull(tableParams.getTableOptions(), "Table options can not be null.");
        List<TableChange> tableChanges = new ArrayList<>();
        tableParams
            .getTableOptions()
            .forEach(
                (key, value) -> {
                    tableChanges.add(TableChange.set(key, value));
                });

        return flinkCatalogBase.alterTable(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            tableParams.getDatabaseName(),
            tableParams.getName(),
            tableChanges,
            true);
    }

    @Override
    public boolean removeOption(
                                String catalogName, String databaseName, String tableName, String key) {
        AlertException.throwIfNull(tableName, "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(catalogName);
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        AlertException.throwIfNull(key, "Table options key can not be null.");
        List<TableChange> tableChanges = new ArrayList<>();

        tableChanges.add(new TableChange.ResetOption(key));

        return flinkCatalogBase.alterTable(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            databaseName,
            tableName,
            tableChanges,
            true);
    }

    @Override
    public boolean dropTable(String catalogName, String databaseName, String tableName) {
        AlertException.throwIfNull(tableName, "Table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(catalogName);
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        return flinkCatalogBase.dropTable(
            catalogName, getOptions(flinkCatalog.getConfiguration()), databaseName, tableName, true);
    }

    @Override
    public boolean renameTable(
                               String catalogName, String databaseName, String fromTableName, String toTableName) {
        AlertException.throwIfNull(fromTableName, "From table name can not be null.");
        AlertException.throwIfNull(toTableName, "To table name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(catalogName);
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        return flinkCatalogBase.renameTable(
            catalogName,
            getOptions(flinkCatalog.getConfiguration()),
            databaseName,
            fromTableName,
            toTableName);
    }

    @Override
    public List<TableParams> listTables(TableParams tableParams) {
        AlertException.throwIfNull(tableParams.getDatabaseName(), "Database name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(tableParams.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");
        List<String> tables =
            flinkCatalogBase.listTable(
                tableParams.getCatalogName(),
                getOptions(flinkCatalog.getConfiguration()),
                tableParams.getDatabaseName());

        if (tables == null || tables.isEmpty()) {
            return null;
        }
        List<TableParams> tableParamsList = new ArrayList<>();
        tables.forEach(
            tableName -> {
                tableParamsList.add(
                    covertToTableParams(
                        flinkCatalogBase.getTable(
                            flinkCatalog.getCatalogName(),
                            getOptions(flinkCatalog.getConfiguration()),
                            tableParams.getDatabaseName(),
                            tableParams.getName())));
            });

        return tableParamsList;
    }

    @Override
    public TableParams listColumns(String catalogName, String databaseName, String tableName) {
        AlertException.throwIfNull(databaseName, "Database name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(catalogName);
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit.");

        CatalogBaseTable originTable =
            flinkCatalogBase.getTable(
                catalogName, getOptions(flinkCatalog.getConfiguration()), databaseName, tableName);
        TableParams tableParams = covertToTableParams(originTable);
        tableParams.setName(tableName);
        tableParams.setCatalogName(catalogName);
        tableParams.setDatabaseName(catalogName);
        tableParams.setCatalogId(flinkCatalog.getId());
        return tableParams;
    }

    private TableParams covertToTableParams(CatalogBaseTable catalogBaseTable) {
        List<TableColumn> tableColumns = new ArrayList<>();
        catalogBaseTable
            .getUnresolvedSchema()
            .getColumns()
            .forEach(
                unresolvedColumn -> {
                    TableColumn tableColumn = new TableColumn();
                    tableColumn.setField(unresolvedColumn.getName());
                    unresolvedColumn.getComment().ifPresent(tableColumn::setComment);
                    tableColumns.add(tableColumn);
                });
        catalogBaseTable
            .getUnresolvedSchema()
            .getPrimaryKey()
            .ifPresent(
                unresolvedPrimaryKey -> {
                    List<String> primaryKeys = unresolvedPrimaryKey.getColumnNames();
                    for (String primary : primaryKeys) {
                        for (TableColumn column : tableColumns) {
                            if (column.getField().equals(primary)) {
                                column.setPk(true);
                            }
                        }
                    }
                });
        return TableParams.builder().tableColumns(tableColumns).build();
    }
}
