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

package org.apache.streampark.flink.catalog;

import org.apache.streampark.common.tuple.Tuple2;
import org.apache.streampark.common.tuple.Tuple3;
import org.apache.streampark.flink.catalog.dao.ColumnDao;
import org.apache.streampark.flink.catalog.dao.DatabaseDao;
import org.apache.streampark.flink.catalog.dao.DatabasePropertiesDao;
import org.apache.streampark.flink.catalog.dao.FunctionDao;
import org.apache.streampark.flink.catalog.dao.TableDao;
import org.apache.streampark.flink.catalog.dao.TablePropertiesDao;
import org.apache.streampark.flink.catalog.utils.Constants;

import org.apache.flink.table.api.Schema;
import org.apache.flink.table.catalog.AbstractCatalog;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogDatabase;
import org.apache.flink.table.catalog.CatalogDatabaseImpl;
import org.apache.flink.table.catalog.CatalogFunction;
import org.apache.flink.table.catalog.CatalogFunctionImpl;
import org.apache.flink.table.catalog.CatalogPartition;
import org.apache.flink.table.catalog.CatalogPartitionSpec;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.CatalogView;
import org.apache.flink.table.catalog.FunctionLanguage;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.ResolvedCatalogBaseTable;
import org.apache.flink.table.catalog.ResolvedCatalogTable;
import org.apache.flink.table.catalog.ResolvedCatalogView;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotEmptyException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotExistException;
import org.apache.flink.table.catalog.exceptions.FunctionAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.FunctionNotExistException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.stats.CatalogColumnStatistics;
import org.apache.flink.table.catalog.stats.CatalogTableStatistics;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.util.StringUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

@Getter
public class MysqlCatalog extends AbstractCatalog {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final HikariConfig hikariConfig;

  private HikariDataSource dataSource;

  private DatabaseDao databaseDao;

  private DatabasePropertiesDao databasePropertiesDao;

  private TableDao tableDao;

  private TablePropertiesDao tablePropertiesDao;

  private FunctionDao functionDao;

  private ColumnDao columnDao;

  public MysqlCatalog(String name, String jdbcUrl, String username, String password) {

    super(name, Constants.DEFAULT_DATABASE);

    checkArgument(!StringUtils.isNullOrWhitespaceOnly(jdbcUrl));
    checkArgument(!StringUtils.isNullOrWhitespaceOnly(username));
    checkArgument(!StringUtils.isNullOrWhitespaceOnly(password));

    hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);
    hikariConfig.setMaximumPoolSize(5);
    hikariConfig.setConnectionTestQuery("SELECT 1");
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "10");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "20");
  }

  @Override
  public void open() throws CatalogException {
    dataSource = new HikariDataSource(hikariConfig);
    Integer defaultDbId = getDatabaseId(Constants.DEFAULT_DATABASE);
    // check default database, if not exist, create it.
    if (defaultDbId == null) {
      try {
        createDatabase(
            Constants.DEFAULT_DATABASE,
            new CatalogDatabaseImpl(
                new HashMap<>(), "Default database of  StreamPark mysql catalog"),
            true);
        logger.info("create default database: {}", Constants.DEFAULT_DATABASE);
      } catch (DatabaseAlreadyExistException a) {
        // ignore db already exist
      }
    }
    logger.info("open StreamPark mysql catalog");

    databaseDao = new DatabaseDao(dataSource);
    databasePropertiesDao = new DatabasePropertiesDao(dataSource);
    tableDao = new TableDao(dataSource);
    tablePropertiesDao = new TablePropertiesDao(dataSource);
    functionDao = new FunctionDao(dataSource);
    columnDao = new ColumnDao(dataSource);
  }

  @Override
  public void close() throws CatalogException {
    dataSource.close();
    databaseDao = null;
    databasePropertiesDao = null;
    tableDao = null;
    tablePropertiesDao = null;
    functionDao = null;
    columnDao = null;
    logger.info("close StreamPark mysql catalog");
  }

  // ---------------------- databases ----------------------
  @Override
  public List<String> listDatabases() throws CatalogException {
    try {
      return databaseDao.listDatabases();
    } catch (Exception e) {
      throw new CatalogException(
          String.format("failed listing database in catalog %s", getName()), e);
    }
  }

  @Override
  public CatalogDatabase getDatabase(String databaseName) throws CatalogException {
    try {
      Tuple2<Integer, String> tuple2 = databaseDao.getByName(databaseName);
      if (tuple2 == null) {
        throw new DatabaseNotExistException(getName(), databaseName);
      }
      try {
        Map<String, String> map = databasePropertiesDao.getProperties(tuple2.f0);
        return new CatalogDatabaseImpl(map, tuple2.f1);
      } catch (Exception e) {
        throw new CatalogException(
            String.format("failed get database properties in catalog %s", getName()), e);
      }
    } catch (Exception e) {
      throw new CatalogException(String.format("failed get database in catalog %s", getName()), e);
    }
  }

  @Override
  public boolean databaseExists(String databaseName) throws CatalogException {
    return getDatabaseId(databaseName) != null;
  }

  private Integer getDatabaseId(String databaseName) throws CatalogException {
    try {
      return databaseDao.getByName(databaseName).f0;
    } catch (Exception e) {
      throw new CatalogException(
          String.format("failed get database id in catalog %s", getName()), e);
    }
  }

  @Override
  public void createDatabase(String databaseName, CatalogDatabase db, boolean ignoreIfExists)
      throws DatabaseAlreadyExistException, CatalogException {

    checkArgument(!StringUtils.isNullOrWhitespaceOnly(databaseName));
    checkNotNull(db);
    if (databaseExists(databaseName)) {
      if (!ignoreIfExists) {
        throw new DatabaseAlreadyExistException(getName(), databaseName);
      }
    } else {
      try {
        databaseDao.save(databaseName, db.getComment(), db.getProperties());
      } catch (Exception e) {
        logger.error("create database error", e);
      }
    }
  }

  @Override
  public void dropDatabase(String name, boolean ignoreIfNotExists, boolean cascade)
      throws DatabaseNotExistException, DatabaseNotEmptyException, CatalogException {
    if (name.equals(Constants.DEFAULT_DATABASE)) {
      throw new CatalogException("The default database cannot be dropped.");
    }
    Integer id = getDatabaseId(name);
    if (id == null) {
      if (!ignoreIfNotExists) {
        throw new DatabaseNotExistException(getName(), name);
      }
      return;
    }

    try {
      List<String> tables = listTables(name);
      if (!tables.isEmpty()) {
        if (!cascade) {
          throw new DatabaseNotEmptyException(getName(), name);
        }
        for (String table : tables) {
          try {
            dropTable(new ObjectPath(name, table), true);
          } catch (TableNotExistException t) {
            logger.warn("drop table error", t);
          }
        }
      }
      databaseDao.drop(id);
    } catch (SQLException e) {
      throw new CatalogException("delete database error", e);
    }
  }

  @Override
  public void alterDatabase(String name, CatalogDatabase database, boolean ignoreIfNotExists)
      throws DatabaseNotExistException, CatalogException {
    if (name.equals(Constants.DEFAULT_DATABASE)) {
      throw new CatalogException("The default database cannot be altered.");
    }
    Integer id = getDatabaseId(name);
    if (id == null) {
      if (!ignoreIfNotExists) {
        throw new DatabaseNotExistException(getName(), name);
      }
      return;
    }

    try {
      databaseDao.update(database.getComment(), id, database.getProperties());
    } catch (SQLException e) {
      throw new CatalogException("alter database error", e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------tables and views-----------------------

  @Override
  public List<String> listTables(String databaseName)
      throws DatabaseNotExistException, CatalogException {
    return listTablesViews(databaseName, Constants.TABLE);
  }

  @Override
  public List<String> listViews(String databaseName)
      throws DatabaseNotExistException, CatalogException {
    return listTablesViews(databaseName, Constants.VIEW);
  }

  protected List<String> listTablesViews(String databaseName, String tableType)
      throws DatabaseNotExistException, CatalogException {
    Integer databaseId = getDatabaseId(databaseName);
    if (null == databaseId) {
      throw new DatabaseNotExistException(getName(), databaseName);
    }
    try {
      return tableDao.getTableName(tableType, databaseId);
    } catch (Exception e) {
      throw new CatalogException(
          String.format("failed listing %s in catalog %s", tableType, getName()), e);
    }
  }

  @Override
  public CatalogBaseTable getTable(ObjectPath tablePath)
      throws TableNotExistException, CatalogException {
    Integer id = getTableId(tablePath);
    if (id == null) {
      throw new TableNotExistException(getName(), tablePath);
    }

    try {
      Tuple2<String, String> tuple2 = tableDao.getById(id);
      if (tuple2 == null) {
        throw new TableNotExistException(getName(), tablePath);
      }

      String comment = tuple2.f0;
      String tableType = tuple2.f1;

      switch (tableType) {
        case Constants.TABLE:
          Map<String, String> props = tablePropertiesDao.getProperties(id);
          props.put(Constants.COMMENT, comment);
          return CatalogTable.fromProperties(props);

        case Constants.VIEW:
          List<Tuple3<String, String, String>> columns = columnDao.getColumn(id);
          Schema.Builder builder = Schema.newBuilder();
          columns.forEach(
              x -> {
                builder.column(x.f0, x.f1);
                if (x.f2 != null) {
                  builder.withComment(x.f2);
                }
              });

          Map<String, String> options = tablePropertiesDao.getProperties(id);
          String originalQuery = options.remove("OriginalQuery");
          String expandedQuery = options.remove("ExpandedQuery");
          return CatalogView.of(builder.build(), comment, originalQuery, expandedQuery, options);

        default:
          throw new CatalogException("Unknown table type: " + tableType);
      }
    } catch (SQLException e) {
      throw new CatalogException("get table failed.", e);
    }
  }

  @Override
  public boolean tableExists(ObjectPath tablePath) throws CatalogException {
    return getTableId(tablePath) != null;
  }

  private Integer getTableId(ObjectPath tablePath) {
    Integer dbId = getDatabaseId(tablePath.getDatabaseName());
    if (dbId == null) {
      return null;
    }

    try {
      return tableDao.getTableId(tablePath.getObjectName(), dbId);
    } catch (SQLException e) {
      throw new CatalogException("get table failed.", e);
    }
  }

  @Override
  public void dropTable(ObjectPath tablePath, boolean ignoreIfNotExists)
      throws TableNotExistException, CatalogException {
    Integer id = getTableId(tablePath);

    if (id == null) {
      throw new TableNotExistException(getName(), tablePath);
    }
    try {
      tableDao.drop(id);
    } catch (SQLException e) {
      throw new CatalogException("drop table failed.", e);
    }
  }

  @Override
  public void renameTable(ObjectPath tablePath, String newTableName, boolean ignoreIfNotExists)
      throws TableNotExistException, TableAlreadyExistException, CatalogException {
    Integer id = getTableId(tablePath);

    if (id == null) {
      throw new TableNotExistException(getName(), tablePath);
    }
    ObjectPath newPath = new ObjectPath(tablePath.getDatabaseName(), newTableName);
    if (tableExists(newPath)) {
      throw new TableAlreadyExistException(getName(), newPath);
    }
    try {
      tableDao.rename(newTableName, id);
    } catch (SQLException ex) {
      throw new CatalogException("rename table failed.", ex);
    }
  }

  @Override
  public void createTable(ObjectPath tablePath, CatalogBaseTable table, boolean ignoreIfExists)
      throws TableAlreadyExistException, DatabaseNotExistException, CatalogException {
    Integer dbId = getDatabaseId(tablePath.getDatabaseName());
    if (null == dbId) {
      throw new DatabaseNotExistException(getName(), tablePath.getDatabaseName());
    }
    if (tableExists(tablePath)) {
      if (!ignoreIfExists) {
        throw new TableAlreadyExistException(getName(), tablePath);
      }
      return;
    }

    if (!(table instanceof ResolvedCatalogBaseTable)) {
      throw new UnsupportedOperationException("Only support ResolvedCatalogBaseTable");
    }

    try {
      CatalogBaseTable.TableKind kind = table.getTableKind();
      Integer id =
          tableDao.save(tablePath.getObjectName(), kind.toString(), dbId, table.getComment());

      if (table instanceof ResolvedCatalogTable) {
        Map<String, String> props = ((ResolvedCatalogTable) table).toProperties();
        tablePropertiesDao.save(id, props);
      } else {
        ResolvedCatalogView view = (ResolvedCatalogView) table;
        List<Schema.UnresolvedColumn> cols = view.getUnresolvedSchema().getColumns();
        if (!cols.isEmpty()) {
          columnDao.save(id, tablePath.getDatabaseName(), tablePath.getObjectName(), cols);
          Map<String, String> option = view.getOptions();
          if (option == null) {
            option = new HashMap<>();
          }
          option.put("OriginalQuery", view.getOriginalQuery());
          option.put("ExpandedQuery", view.getExpandedQuery());
          tablePropertiesDao.save(id, option);
        }
      }
    } catch (Exception ex) {
      throw new CatalogException("insert table failed.", ex);
    }
  }

  @Override
  public void alterTable(ObjectPath tablePath, CatalogBaseTable newTable, boolean ignoreIfNotExists)
      throws TableNotExistException, CatalogException {

    Integer id = getTableId(tablePath);
    if (id == null) {
      throw new TableNotExistException(getName(), tablePath);
    }
    Map<String, String> opts = newTable.getOptions();
    if (mapNotEmpty(opts)) {
      try {
        tablePropertiesDao.upsert(id, opts);
      } catch (Exception ex) {
        throw new CatalogException("alter table failed.", ex);
      }
    }
  }

  // --------------------------- function ----------------------------
  @Override
  public List<String> listFunctions(String dbName)
      throws DatabaseNotExistException, CatalogException {
    Integer dbId = getDatabaseId(dbName);
    if (null == dbId) {
      throw new DatabaseNotExistException(getName(), dbName);
    }
    try {
      return functionDao.listFunctions(dbId);
    } catch (Exception e) {
      throw new CatalogException("list functions failed.", e);
    }
  }

  @Override
  public CatalogFunction getFunction(ObjectPath functionPath)
      throws FunctionNotExistException, CatalogException {
    Integer id = getFunctionId(functionPath);
    if (null == id) {
      throw new FunctionNotExistException(getName(), functionPath);
    }
    try {
      Tuple2<String, String> function = functionDao.getFunction(id);
      if (function == null) {
        throw new FunctionNotExistException(getName(), functionPath);
      }
      return new CatalogFunctionImpl(function.f0, FunctionLanguage.valueOf(function.f1));
    } catch (Exception e) {
      throw new CatalogException("get function failed.", e);
    }
  }

  @Override
  public boolean functionExists(ObjectPath functionPath) throws CatalogException {
    return getFunctionId(functionPath) != null;
  }

  private Integer getFunctionId(ObjectPath functionPath) {
    Integer dbId = getDatabaseId(functionPath.getDatabaseName());
    if (dbId == null) {
      return null;
    }
    try {
      return functionDao.getId(dbId, functionPath.getObjectName());
    } catch (Exception e) {
      logger.error("get function failed", e);
      throw new CatalogException("get function failed.", e);
    }
  }

  @Override
  public void createFunction(
      ObjectPath functionPath, CatalogFunction function, boolean ignoreIfExists)
      throws FunctionAlreadyExistException, DatabaseNotExistException, CatalogException {
    Integer dbId = getDatabaseId(functionPath.getDatabaseName());
    if (null == dbId) {
      throw new DatabaseNotExistException(getName(), functionPath.getDatabaseName());
    }
    if (functionExists(functionPath)) {
      if (!ignoreIfExists) {
        throw new FunctionAlreadyExistException(getName(), functionPath);
      }
      return;
    }
    try {
      functionDao.save(
          functionPath.getObjectName(),
          function.getClassName(),
          dbId,
          function.getFunctionLanguage().toString());
    } catch (Exception e) {
      throw new CatalogException("create function failed.", e);
    }
  }

  @Override
  public void alterFunction(
      ObjectPath functionPath, CatalogFunction newFunction, boolean ignoreIfNotExists)
      throws FunctionNotExistException, CatalogException {
    Integer id = getFunctionId(functionPath);
    if (null == id) {
      if (!ignoreIfNotExists) {
        throw new FunctionNotExistException(getName(), functionPath);
      }
      return;
    }
    try {
      functionDao.update(
          id, newFunction.getClassName(), newFunction.getFunctionLanguage().toString());
    } catch (Exception e) {
      throw new CatalogException("alter function failed.", e);
    }
  }

  @Override
  public void dropFunction(ObjectPath functionPath, boolean ignoreIfNotExists)
      throws FunctionNotExistException, CatalogException {

    Integer id = getFunctionId(functionPath);
    if (null == id) {
      if (!ignoreIfNotExists) {
        throw new FunctionNotExistException(getName(), functionPath);
      }
      return;
    }
    try {
      functionDao.drop(id);
    } catch (Exception e) {
      throw new CatalogException("drop function failed.", e);
    }
  }

  // ----------------------------- partition -----------------------------
  @Override
  public List<CatalogPartitionSpec> listPartitions(ObjectPath tablePath) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitions(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitionsByFilter(
      ObjectPath tablePath, List<Expression> filters) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CatalogPartition getPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean partitionExists(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createPartition(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogPartition partition,
      boolean ignoreIfExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dropPartition(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartition(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogPartition newPartition,
      boolean ignoreIfNotExists)
      throws CatalogException {

    throw new UnsupportedOperationException();
  }

  // ------------------------------ statistics ------------------------------

  @Override
  public CatalogTableStatistics getTableStatistics(ObjectPath tablePath) throws CatalogException {
    return CatalogTableStatistics.UNKNOWN;
  }

  @Override
  public CatalogColumnStatistics getTableColumnStatistics(ObjectPath tablePath)
      throws CatalogException {
    return CatalogColumnStatistics.UNKNOWN;
  }

  @Override
  public CatalogTableStatistics getPartitionStatistics(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CatalogColumnStatistics getPartitionColumnStatistics(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterTableStatistics(
      ObjectPath tablePath, CatalogTableStatistics tableStatistics, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterTableColumnStatistics(
      ObjectPath tablePath, CatalogColumnStatistics columnStatistics, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartitionStatistics(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogTableStatistics partitionStatistics,
      boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartitionColumnStatistics(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogColumnStatistics columnStatistics,
      boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  private boolean mapNotEmpty(Map<?, ?> map) {
    return map == null || map.isEmpty();
  }
}
