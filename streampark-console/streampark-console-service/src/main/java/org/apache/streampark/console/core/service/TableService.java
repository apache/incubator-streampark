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

import org.apache.streampark.console.core.bean.TableParams;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TableService {

    /**
     * Checks if the specified table exists.
     *
     * @param tableParams The TableParams object containing information about the table
     * @return true if the table exists, false otherwise
     */
    boolean tableExists(TableParams tableParams);

    /**
     * Creates a table in the database given ${@link TableParams}.
     *
     * @param tableParams The TableParams object containing information about the table
     * @return true if the operation is successful, false otherwise
     */
    boolean createTable(TableParams tableParams);

    /**
     * Adds a column to the table.
     *
     * @param tableParams The TableDTO object containing information about the table
     * @return true if the operation is successful, false otherwise
     */
    boolean addColumn(TableParams tableParams);

    /**
     * Drops a column from a table.
     *
     * @param catalogName The name of the catalog
     * @param databaseName The name of the database
     * @param tableName The name of the table
     * @param columnName The name of the column to be dropped
     * @return true if the operation is successful, false otherwise
     */
    boolean dropColumn(String catalogName, String databaseName, String tableName, String columnName);

    /**
     * Adds options to a table.
     *
     * @param tableDTO The TableDTO object containing information about the table
     * @return true if the operation is successful, false otherwise
     */
    boolean addOption(TableParams tableDTO);

    /**
     * Removes an option from a table.
     *
     * @param catalogName The name of the catalog
     * @param databaseName The name of the database
     * @param tableName The name of the table
     * @param key The key of the option to be removed
     * @return true if the operation is successful, false otherwise
     */
    boolean removeOption(String catalogName, String databaseName, String tableName, String key);

    /**
     * Drops a table from the specified database in the given catalog.
     *
     * @param catalogName The name of the catalog from which the table will be dropped
     * @param databaseName The name of the database from which the table will be dropped
     * @param tableName The name of the table to be dropped
     * @return true if the operation is successful, false otherwise
     */
    boolean dropTable(String catalogName, String databaseName, String tableName);

    /**
     * Renames a table in the specified database of the given catalog.
     *
     * @param catalogName The name of the catalog where the table resides
     * @param databaseName The name of the database where the table resides
     * @param fromTableName The current name of the table to be renamed
     * @param toTableName The new name for the table
     * @return true if the operation is successful, false otherwise
     */
    boolean renameTable(
                        String catalogName, String databaseName, String fromTableName, String toTableName);

    /**
     * Lists tables given {@link TableParams} condition.
     *
     * @return Response object containing a list of {@link TableParams} representing the tables
     */
    List<TableParams> listTables(TableParams tableDTO);

    /**
     * Retrieves the column details of a specific table within the specified catalog and database.
     *
     * @param catalogName The name of the catalog where the table is located
     * @param databaseName The name of the database where the table is located
     * @param tableName The name of the table whose columns are to be retrieved
     * @return A {@link TableParams} object containing the details of the columns of the specified
     *     table
     */
    TableParams listColumns(String catalogName, String databaseName, String tableName);
}
