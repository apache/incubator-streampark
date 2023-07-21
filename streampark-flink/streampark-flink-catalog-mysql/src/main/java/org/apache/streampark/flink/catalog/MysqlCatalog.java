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

import org.apache.streampark.flink.catalog.utils.Constants;

import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
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
import org.apache.flink.table.catalog.exceptions.PartitionAlreadyExistsException;
import org.apache.flink.table.catalog.exceptions.PartitionNotExistException;
import org.apache.flink.table.catalog.exceptions.PartitionSpecInvalidException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.exceptions.TableNotPartitionedException;
import org.apache.flink.table.catalog.exceptions.TablePartitionedException;
import org.apache.flink.table.catalog.stats.CatalogColumnStatistics;
import org.apache.flink.table.catalog.stats.CatalogTableStatistics;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.types.DataType;
import org.apache.flink.util.StringUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.ArrayList;
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
  }

  @Override
  public void close() throws CatalogException {
    dataSource.close();
    logger.info("close StreamPark mysql catalog");
  }

  // ---------------------- databases ----------------------
  @Override
  public List<String> listDatabases() throws CatalogException {
    List<String> myDatabases = new ArrayList<>();
    try {
      String querySql = "SELECT database_name FROM metadata_database";
      jdbcQuery(
          querySql,
          null,
          resultSet -> {
            while (resultSet.next()) {
              String dbName = resultSet.getString(1);
              myDatabases.add(dbName);
            }
          });
    } catch (Exception e) {
      throw new CatalogException(
          String.format("Failed listing database in catalog %s", getName()), e);
    }
    return myDatabases;
  }

  @Override
  public CatalogDatabase getDatabase(String databaseName)
      throws DatabaseNotExistException, CatalogException {

    Map<String, String> map = new HashMap<>();

    String sql = "SELECT id,database_name,comment FROM metadata_database where database_name=?";
    try {
      Tuple2<Integer, String> tuple2 = new Tuple2<>();
      jdbcQuery(
          sql,
          s -> s.setString(1, databaseName),
          r -> {
            if (r.next()) {
              tuple2.setFields(r.getInt("id"), r.getString("comment"));
            } else {
              throw new DatabaseNotExistException(getName(), databaseName);
            }
          });

      sql = "select `key`,`value` from metadata_database_property where database_id=? ";

      try {
        jdbcQuery(
            sql,
            s -> s.setInt(1, tuple2.f0),
            r -> {
              while (r.next()) {
                map.put(r.getString("key"), r.getString("value"));
              }
            });
        return new CatalogDatabaseImpl(map, tuple2.f1);
      } catch (Exception e) {
        throw new CatalogException(
            String.format("Failed get database properties in catalog %s", getName()), e);
      }
    } catch (Exception e) {
      throw new CatalogException(String.format("Failed get database in catalog %s", getName()), e);
    }
  }

  @Override
  public boolean databaseExists(String databaseName) throws CatalogException {
    return getDatabaseId(databaseName) != null;
  }

  private Integer getDatabaseId(String databaseName) throws CatalogException {
    String querySql = "select id from metadata_database where database_name=?";
    Tuple1<Integer> tuple1 = new Tuple1<>();
    try {
      jdbcQuery(
          querySql,
          statement -> statement.setString(1, databaseName),
          resultSet -> tuple1.setField(resultSet.next() ? resultSet.getInt("id") : null, 0));
    } catch (Exception e) {
      throw new CatalogException(
          String.format("Failed get database id in catalog %s", getName()), e);
    }
    return tuple1.f0;
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
      String insertSql = "insert into metadata_database(database_name, comment) values(?, ?)";
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stat =
              conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
        conn.setAutoCommit(false);
        stat.setString(1, databaseName);
        stat.setString(2, db.getComment());
        stat.executeUpdate();
        ResultSet idRs = stat.getGeneratedKeys();
        if (idRs.next() && db.getProperties() != null && db.getProperties().size() > 0) {
          int id = idRs.getInt(1);
          String propInsertSql =
              "insert into metadata_database_property(database_id, "
                  + "`key`,`value`) values (?,?,?)";
          PreparedStatement pstat = conn.prepareStatement(propInsertSql);
          for (Map.Entry<String, String> entry : db.getProperties().entrySet()) {
            pstat.setInt(1, id);
            pstat.setString(2, entry.getKey());
            pstat.setString(3, entry.getValue());
            pstat.addBatch();
          }
          pstat.executeBatch();
          pstat.close();
        }
        conn.commit();
      } catch (SQLException e) {
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

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      List<String> tables = listTables(name);
      if (tables.size() > 0) {
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
      String deletePropSql = "delete from metadata_database_property where database_id=?";
      PreparedStatement dStat = conn.prepareStatement(deletePropSql);
      dStat.setInt(1, id);
      dStat.executeUpdate();
      dStat.close();
      String deleteDbSql = "delete from metadata_database where id=?";
      dStat = conn.prepareStatement(deleteDbSql);
      dStat.setInt(1, id);
      dStat.executeUpdate();
      dStat.close();
      conn.commit();
    } catch (SQLException e) {
      throw new CatalogException("delete database error", e);
    }
  }

  @Override
  public void alterDatabase(String name, CatalogDatabase newDb, boolean ignoreIfNotExists)
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

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      String updateCommentSql = "update metadata_database set comment=? where id=?";
      PreparedStatement uState = conn.prepareStatement(updateCommentSql);
      uState.setString(1, newDb.getComment());
      uState.setInt(2, id);
      uState.executeUpdate();
      uState.close();
      if (newDb.getProperties() != null && newDb.getProperties().size() > 0) {
        String upsertSql =
            "insert  into metadata_database_property (database_id, `key`,`value`) \n"
                + "values (?,?,?)\n"
                + "on duplicate key update `value` =?, update_time = sysdate()\n";
        PreparedStatement pstat = conn.prepareStatement(upsertSql);
        for (Map.Entry<String, String> entry : newDb.getProperties().entrySet()) {
          pstat.setInt(1, id);
          pstat.setString(2, entry.getKey());
          pstat.setString(3, entry.getValue());
          pstat.setString(4, entry.getValue());
          pstat.addBatch();
        }

        pstat.executeBatch();
      }
      conn.commit();
    } catch (SQLException e) {
      throw new CatalogException("alter database error", e);
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

    String querySql =
        "SELECT table_name FROM metadata_table where table_type=? and database_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(querySql)) {
      ps.setString(1, tableType);
      ps.setInt(2, databaseId);
      ResultSet rs = ps.executeQuery();

      List<String> tables = new ArrayList<>();
      while (rs.next()) {
        String table = rs.getString(1);
        tables.add(table);
      }
      return tables;
    } catch (Exception e) {
      throw new CatalogException(
          String.format("Failed listing %s in catalog %s", tableType, getName()), e);
    }
  }

  @Override
  public CatalogBaseTable getTable(ObjectPath tablePath)
      throws TableNotExistException, CatalogException {
    Integer id = getTableId(tablePath);
    if (id == null) {
      throw new TableNotExistException(getName(), tablePath);
    }

    try (Connection conn = dataSource.getConnection()) {
      String queryTable =
          "SELECT table_name "
              + " ,comment, table_type "
              + " FROM metadata_table "
              + " where  id=?";
      PreparedStatement ps = conn.prepareStatement(queryTable);
      ps.setInt(1, id);
      ResultSet rs = ps.executeQuery();
      String comment;
      String tableType;
      if (rs.next()) {
        comment = rs.getString("comment");
        tableType = rs.getString("table_type");
        ps.close();
      } else {
        ps.close();
        throw new TableNotExistException(getName(), tablePath);
      }
      if (tableType.equals(Constants.TABLE)) {
        // 这个是 table
        String propSql = "SELECT `key`, `value` from metadata_table_property " + "WHERE table_id=?";
        PreparedStatement pState = conn.prepareStatement(propSql);
        pState.setInt(1, id);
        ResultSet prs = pState.executeQuery();
        Map<String, String> props = new HashMap<>();
        while (prs.next()) {
          String key = prs.getString("key");
          String value = prs.getString("value");
          props.put(key, value);
        }
        pState.close();
        props.put(Constants.COMMENT, comment);
        return CatalogTable.fromProperties(props);
      } else if (tableType.equals(Constants.VIEW)) {
        String colSql =
            "SELECT column_name, column_type, data_type, comment "
                + " FROM metadata_column WHERE "
                + " table_id=?";
        PreparedStatement cStat = conn.prepareStatement(colSql);
        cStat.setInt(1, id);
        ResultSet crs = cStat.executeQuery();

        Schema.Builder builder = Schema.newBuilder();
        while (crs.next()) {
          String colName = crs.getString("column_name");
          String dataType = crs.getString("data_type");

          builder.column(colName, dataType);
          String cDesc = crs.getString("comment");
          if (null != cDesc && cDesc.length() > 0) {
            builder.withComment(cDesc);
          }
        }
        cStat.close();
        String qSql = "SELECT `key`, value FROM metadata_table_property" + " WHERE table_id=? ";
        PreparedStatement qStat = conn.prepareStatement(qSql);
        qStat.setInt(1, id);
        ResultSet qrs = qStat.executeQuery();
        String originalQuery = "";
        String expandedQuery = "";
        Map<String, String> options = new HashMap<>();
        while (qrs.next()) {
          String key = qrs.getString("key");
          String value = qrs.getString("value");
          if ("OriginalQuery".equals(key)) {
            originalQuery = value;
          } else if ("ExpandedQuery".equals(key)) {
            expandedQuery = value;
          } else {
            options.put(key, value);
          }
        }
        return CatalogView.of(builder.build(), comment, originalQuery, expandedQuery, options);
      } else {
        throw new CatalogException("Unknown table type: " + tableType);
      }
    } catch (SQLException e) {
      throw new CatalogException("get table fail.", e);
    }
  }

  @Override
  public boolean tableExists(ObjectPath tablePath) throws CatalogException {
    Integer id = getTableId(tablePath);
    return id != null;
  }

  private Integer getTableId(ObjectPath tablePath) {
    Integer dbId = getDatabaseId(tablePath.getDatabaseName());
    if (dbId == null) {
      return null;
    }
    String getIdSql = "select id from metadata_table " + " where table_name=? and database_id=?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement gStat = conn.prepareStatement(getIdSql)) {
      gStat.setString(1, tablePath.getObjectName());
      gStat.setInt(2, dbId);
      ResultSet rs = gStat.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      throw new CatalogException("get table fail.", e);
    }
    return null;
  }

  @Override
  public void dropTable(ObjectPath tablePath, boolean ignoreIfNotExists)
      throws TableNotExistException, CatalogException {
    Integer id = getTableId(tablePath);

    if (id == null) {
      throw new TableNotExistException(getName(), tablePath);
    }

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      String deletePropSql = "delete from metadata_table_property " + " where table_id=?";
      PreparedStatement dStat = conn.prepareStatement(deletePropSql);
      dStat.setInt(1, id);
      dStat.executeUpdate();
      dStat.close();
      String deleteColSql = "delete from metadata_column " + " where table_id=?";
      dStat = conn.prepareStatement(deleteColSql);
      dStat.setInt(1, id);
      dStat.executeUpdate();
      dStat.close();
      String deleteDbSql = "delete from metadata_table " + " where id=?";
      dStat = conn.prepareStatement(deleteDbSql);
      dStat.setInt(1, id);
      dStat.executeUpdate();
      dStat.close();
      conn.commit();
    } catch (SQLException e) {
      throw new CatalogException("drop table fail.", e);
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
    String updateSql = "UPDATE metadata_table SET table_name=? WHERE id=?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(updateSql)) {
      ps.setString(1, newTableName);
      ps.setInt(2, id);
      ps.executeUpdate();
    } catch (SQLException ex) {
      throw new CatalogException("rename table fail.", ex);
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

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      CatalogBaseTable.TableKind kind = table.getTableKind();
      String insertSql =
          "insert into metadata_table(\n"
              + " table_name,"
              + " table_type,"
              + " database_id,"
              + " comment)"
              + " values(?,?,?,?)";
      PreparedStatement iStat = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
      iStat.setString(1, tablePath.getObjectName());
      iStat.setString(2, kind.toString());
      iStat.setInt(3, dbId);
      iStat.setString(4, table.getComment());
      iStat.executeUpdate();
      ResultSet idRs = iStat.getGeneratedKeys();
      if (!idRs.next()) {
        iStat.close();
        throw new CatalogException("insert table fail.");
      }
      int id = idRs.getInt(1);
      iStat.close();
      if (table instanceof ResolvedCatalogTable) {
        Map<String, String> props = ((ResolvedCatalogTable) table).toProperties();
        String propInsertSql =
            "insert into metadata_table_property(table_id," + "`key`,`value`) values (?,?,?)";
        PreparedStatement pStat = conn.prepareStatement(propInsertSql);
        for (Map.Entry<String, String> entry : props.entrySet()) {
          pStat.setInt(1, id);
          pStat.setString(2, entry.getKey());
          pStat.setString(3, entry.getValue());
          pStat.addBatch();
        }
        pStat.executeBatch();
        pStat.close();
      } else {
        ResolvedCatalogView view = (ResolvedCatalogView) table;
        List<Schema.UnresolvedColumn> cols = view.getUnresolvedSchema().getColumns();
        if (cols.size() > 0) {
          String colInsertSql =
              "insert into metadata_column("
                  + " column_name, column_type, data_type"
                  + " , `expr`"
                  + " , comment"
                  + " , table_id"
                  + " , `primary`) "
                  + " values(?,?,?,?,?,?,?)";
          PreparedStatement colIStat = conn.prepareStatement(colInsertSql);
          for (Schema.UnresolvedColumn col : cols) {
            if (col instanceof Schema.UnresolvedPhysicalColumn) {
              Schema.UnresolvedPhysicalColumn pCol = (Schema.UnresolvedPhysicalColumn) col;
              if (!(pCol.getDataType() instanceof DataType)) {
                throw new UnsupportedOperationException(
                    String.format(
                        "View %s.%s.%s.%s data type %s is not supported",
                        tablePath.getDatabaseName(),
                        tablePath.getObjectName(),
                        pCol.getName(),
                        pCol.getDataType()));
              }
              DataType dataType = (DataType) pCol.getDataType();

              colIStat.setString(1, pCol.getName());
              colIStat.setString(2, Constants.PHYSICAL);
              colIStat.setString(3, dataType.getLogicalType().asSerializableString());
              colIStat.setObject(4, null);
              colIStat.setString(5, pCol.getComment().orElse(""));
              colIStat.setInt(6, id);
              colIStat.setObject(7, null);
              colIStat.addBatch();
            } else {
              throw new UnsupportedOperationException("View does not support virtual columns");
            }
          }
          colIStat.executeBatch();
          colIStat.close();

          Map<String, String> option = view.getOptions();
          if (option == null) {
            option = new HashMap<>();
          }
          option.put("OriginalQuery", view.getOriginalQuery());
          option.put("ExpandedQuery", view.getExpandedQuery());
          String propInsertSql =
              "insert into metadata_table_property(table_id," + "`key`,`value`) values (?,?,?)";
          PreparedStatement pStat = conn.prepareStatement(propInsertSql);
          for (Map.Entry<String, String> entry : option.entrySet()) {
            pStat.setInt(1, id);
            pStat.setString(2, entry.getKey());
            pStat.setString(3, entry.getValue());
            pStat.addBatch();
          }
          pStat.executeBatch();
          pStat.close();
        }
      }
      conn.commit();
    } catch (Exception ex) {
      throw new CatalogException("insert table fail.", ex);
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
    if (opts != null && opts.size() > 0) {
      String updateSql =
          "INSERT INTO metadata_table_property(table_id,"
              + "`key`,`value`) values (?,?,?) "
              + "on duplicate key update `value` =?, update_time = sysdate()";

      try (Connection conn = dataSource.getConnection();
          PreparedStatement ps = conn.prepareStatement(updateSql)) {
        for (Map.Entry<String, String> entry : opts.entrySet()) {
          ps.setInt(1, id);
          ps.setString(2, entry.getKey());
          ps.setString(3, entry.getValue());
          ps.setString(4, entry.getValue());
          ps.addBatch();
        }
        ps.executeBatch();
      } catch (SQLException ex) {
        throw new CatalogException("alter table fail.", ex);
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
    String querySql = "SELECT function_name from metadata_function " + " WHERE database_id=?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement gStat = conn.prepareStatement(querySql)) {
      gStat.setInt(1, dbId);
      ResultSet rs = gStat.executeQuery();
      List<String> functions = new ArrayList<>();
      while (rs.next()) {
        String n = rs.getString("function_name");
        functions.add(n);
      }
      return functions;
    } catch (SQLException e) {
      throw new CatalogException("list functions fail.", e);
    }
  }

  @Override
  public CatalogFunction getFunction(ObjectPath functionPath)
      throws FunctionNotExistException, CatalogException {
    Integer id = getFunctionId(functionPath);
    if (null == id) {
      throw new FunctionNotExistException(getName(), functionPath);
    }

    String querySql = "SELECT class_name,function_language from metadata_function " + " WHERE id=?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement gStat = conn.prepareStatement(querySql)) {
      gStat.setInt(1, id);
      ResultSet rs = gStat.executeQuery();
      if (rs.next()) {
        String className = rs.getString("class_name");
        String language = rs.getString("function_language");
        return new CatalogFunctionImpl(className, FunctionLanguage.valueOf(language));
      } else {
        throw new FunctionNotExistException(getName(), functionPath);
      }
    } catch (SQLException e) {
      throw new CatalogException("get function fail.", e);
    }
  }

  @Override
  public boolean functionExists(ObjectPath functionPath) throws CatalogException {
    Integer id = getFunctionId(functionPath);
    return id != null;
  }

  private Integer getFunctionId(ObjectPath functionPath) {
    Integer dbId = getDatabaseId(functionPath.getDatabaseName());
    if (dbId == null) {
      return null;
    }
    String getIdSql =
        "select id from metadata_function " + " where function_name=? and database_id=?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement gStat = conn.prepareStatement(getIdSql)) {
      gStat.setString(1, functionPath.getObjectName());
      gStat.setInt(2, dbId);
      ResultSet rs = gStat.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      logger.error("get function fail", e);
      throw new CatalogException("get function fail.", e);
    }
    return null;
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

    String insertSql =
        "Insert into metadata_function "
            + "(function_name,class_name,database_id,function_language) "
            + " values (?,?,?,?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(insertSql)) {
      ps.setString(1, functionPath.getObjectName());
      ps.setString(2, function.getClassName());
      ps.setInt(3, dbId);
      ps.setString(4, function.getFunctionLanguage().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new CatalogException("create function fail.", e);
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

    String insertSql =
        "update metadata_function " + "set class_name =?, function_language=? " + " where id=?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(insertSql)) {
      ps.setString(1, newFunction.getClassName());
      ps.setString(2, newFunction.getFunctionLanguage().toString());
      ps.setInt(3, id);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new CatalogException("alter function fail.", e);
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

    String insertSql = "delete from metadata_function " + " where id=?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(insertSql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new CatalogException("drop function fail.", e);
    }
  }

  // ----------------------------- partition -----------------------------
  @Override
  public List<CatalogPartitionSpec> listPartitions(ObjectPath tablePath)
      throws TableNotExistException, TableNotPartitionedException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitions(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws TableNotExistException, TableNotPartitionedException, PartitionSpecInvalidException,
          CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitionsByFilter(
      ObjectPath tablePath, List<Expression> filters)
      throws TableNotExistException, TableNotPartitionedException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CatalogPartition getPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws PartitionNotExistException, CatalogException {
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
      throws TableNotExistException, TableNotPartitionedException, PartitionSpecInvalidException,
          PartitionAlreadyExistsException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dropPartition(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec, boolean ignoreIfNotExists)
      throws PartitionNotExistException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartition(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogPartition newPartition,
      boolean ignoreIfNotExists)
      throws PartitionNotExistException, CatalogException {

    throw new UnsupportedOperationException();
  }

  // ------------------------------ statistics ------------------------------

  @Override
  public CatalogTableStatistics getTableStatistics(ObjectPath tablePath)
      throws TableNotExistException, CatalogException {
    return CatalogTableStatistics.UNKNOWN;
  }

  @Override
  public CatalogColumnStatistics getTableColumnStatistics(ObjectPath tablePath)
      throws TableNotExistException, CatalogException {
    return CatalogColumnStatistics.UNKNOWN;
  }

  @Override
  public CatalogTableStatistics getPartitionStatistics(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws PartitionNotExistException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CatalogColumnStatistics getPartitionColumnStatistics(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws PartitionNotExistException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterTableStatistics(
      ObjectPath tablePath, CatalogTableStatistics tableStatistics, boolean ignoreIfNotExists)
      throws TableNotExistException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterTableColumnStatistics(
      ObjectPath tablePath, CatalogColumnStatistics columnStatistics, boolean ignoreIfNotExists)
      throws TableNotExistException, CatalogException, TablePartitionedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartitionStatistics(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogTableStatistics partitionStatistics,
      boolean ignoreIfNotExists)
      throws PartitionNotExistException, CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartitionColumnStatistics(
      ObjectPath tablePath,
      CatalogPartitionSpec partitionSpec,
      CatalogColumnStatistics columnStatistics,
      boolean ignoreIfNotExists)
      throws PartitionNotExistException, CatalogException {
    throw new UnsupportedOperationException();
  }

  private void jdbcQuery(
      String sql,
      JdbcCallBack<PreparedStatement> statementCallBack,
      JdbcCallBack<ResultSet> resultSetCallBack)
      throws Exception {
    Connection connection = dataSource.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    if (statementCallBack != null) {
      statementCallBack.call(statement);
    }
    ResultSet resultSet = statement.executeQuery(sql);
    if (resultSet != null) {
      resultSetCallBack.call(resultSet);
    }
    close(resultSet, statement, connection);
  }

  private int jdbcUpdate(String sql, JdbcCallBack<PreparedStatement> statementCallBack)
      throws Exception {
    Connection connection = dataSource.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    if (statementCallBack != null) {
      statementCallBack.call(statement);
    }
    int result = statement.executeUpdate(sql);
    close(statement, connection);
    return result;
  }

  public interface JdbcCallBack<T extends Wrapper> {
    void call(T wrapper) throws Exception;
  }

  public void close(AutoCloseable... closes) throws Exception {
    for (AutoCloseable closeable : closes) {
      if (closeable != null) {
        closeable.close();
      }
    }
  }
}
