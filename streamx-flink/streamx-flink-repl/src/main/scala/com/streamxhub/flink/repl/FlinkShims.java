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

package com.streamxhub.flink.repl;


import com.streamxhub.flink.repl.sql.SqlCommandParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.compress.utils.Lists;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.scala.DataSet;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.internal.StreamTableEnvironmentImpl;
import org.apache.flink.table.api.bridge.scala.BatchTableEnvironment;
import org.apache.flink.table.api.config.ExecutionConfigOptions;
import org.apache.flink.table.api.config.OptimizerConfigOptions;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.api.internal.CatalogTableSchemaResolver;
import org.apache.flink.table.api.internal.TableEnvironmentInternal;
import org.apache.flink.table.catalog.CatalogManager;
import org.apache.flink.table.catalog.GenericInMemoryCatalog;
import org.apache.flink.table.delegation.Parser;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.TableAggregateFunction;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.table.operations.*;
import org.apache.flink.table.operations.ddl.*;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * This is abstract class for anything that is api incompatible between different flink versions. It will
 * load the correct version of FlinkShims based on the version of flink.
 */
public class FlinkShims {

  private static final Logger LOGGER = LoggerFactory.getLogger(FlinkShims.class);

  private static FlinkShims flinkShims;

  protected Properties properties;

  public static final AttributedString MESSAGE_HELP = new AttributedStringBuilder()
          .append("The following commands are available:\n\n")
          .append(formatCommand(SqlCommandParser.SqlCommand.CREATE_TABLE, "Create table under current catalog and database."))
          .append(formatCommand(SqlCommandParser.SqlCommand.DROP_TABLE, "Drop table with optional catalog and database. Syntax: 'DROP TABLE [IF EXISTS] <name>;'"))
          .append(formatCommand(SqlCommandParser.SqlCommand.CREATE_VIEW, "Creates a virtual table from a SQL query. Syntax: 'CREATE VIEW <name> AS <query>;'"))
          .append(formatCommand(SqlCommandParser.SqlCommand.DESCRIBE, "Describes the schema of a table with the given name."))
          .append(formatCommand(SqlCommandParser.SqlCommand.DROP_VIEW, "Deletes a previously created virtual table. Syntax: 'DROP VIEW <name>;'"))
          .append(formatCommand(SqlCommandParser.SqlCommand.EXPLAIN, "Describes the execution plan of a query or table with the given name."))
          .append(formatCommand(SqlCommandParser.SqlCommand.HELP, "Prints the available commands."))
          .append(formatCommand(SqlCommandParser.SqlCommand.INSERT_INTO, "Inserts the results of a SQL SELECT query into a declared table sink."))
          .append(formatCommand(SqlCommandParser.SqlCommand.INSERT_OVERWRITE, "Inserts the results of a SQL SELECT query into a declared table sink and overwrite existing data."))
          .append(formatCommand(SqlCommandParser.SqlCommand.SELECT, "Executes a SQL SELECT query on the Flink cluster."))
          .append(formatCommand(SqlCommandParser.SqlCommand.SET, "Sets a session configuration property. Syntax: 'SET <key>=<value>;'. Use 'SET;' for listing all properties."))
          .append(formatCommand(SqlCommandParser.SqlCommand.SHOW_FUNCTIONS, "Shows all user-defined and built-in functions."))
          .append(formatCommand(SqlCommandParser.SqlCommand.SHOW_TABLES, "Shows all registered tables."))
          .append(formatCommand(SqlCommandParser.SqlCommand.SOURCE, "Reads a SQL SELECT query from a file and executes it on the Flink cluster."))
          .append(formatCommand(SqlCommandParser.SqlCommand.USE_CATALOG, "Sets the current catalog. The current database is set to the catalog's default one. Experimental! Syntax: 'USE CATALOG <name>;'"))
          .append(formatCommand(SqlCommandParser.SqlCommand.USE, "Sets the current default database. Experimental! Syntax: 'USE <name>;'"))
          .style(AttributedStyle.DEFAULT.underline())
          .append("\nHint")
          .style(AttributedStyle.DEFAULT)
          .append(": Make sure that a statement ends with ';' for finalizing (multi-line) statements.")
          .toAttributedString();

  private Map<String, StatementSet> statementSetMap = new ConcurrentHashMap<>();

  public FlinkShims(Properties properties) {
    this.properties = properties;
  }

  private static FlinkShims loadShims(FlinkVersion flinkVersion, Properties properties)
      throws Exception {
    return new FlinkShims(properties);
  }

  /**
   *
   * @param flinkVersion
   * @param properties
   * @return
   */
  public static FlinkShims getInstance(FlinkVersion flinkVersion,
                                       Properties properties) throws Exception {
    if (flinkShims == null) {
      flinkShims = loadShims(flinkVersion, properties);
    }
    return flinkShims;
  }

  protected static AttributedString formatCommand(SqlCommandParser.SqlCommand cmd, String description) {
    return new AttributedStringBuilder()
            .style(AttributedStyle.DEFAULT.bold())
            .append(cmd.toString())
            .append("\t\t")
            .style(AttributedStyle.DEFAULT)
            .append(description)
            .append('\n')
            .toAttributedString();
  }


  public Object createCatalogManager(Object config) {
    return CatalogManager.newBuilder()
            .classLoader(Thread.currentThread().getContextClassLoader())
            .config((ReadableConfig) config)
            .defaultCatalog("default_catalog",
                    new GenericInMemoryCatalog("default_catalog", "default_database"))
            .build();
  }

  public String getPyFlinkPythonPath(Properties properties) throws IOException {
    String flinkHome = System.getenv("FLINK_HOME");
    if (flinkHome != null) {
      List<File> depFiles = null;
      depFiles = Arrays.asList(new File(flinkHome + "/opt/python").listFiles());
      StringBuilder builder = new StringBuilder();
      for (File file : depFiles) {
        LOGGER.info("Adding extracted file to PYTHONPATH: " + file.getAbsolutePath());
        builder.append(file.getAbsolutePath() + ":");
      }
      return builder.toString();
    } else {
      throw new IOException("No FLINK_HOME is specified");
    }
  }

  public Object getCollectStreamTableSink(InetAddress targetAddress, int targetPort, Object serializer) {
    return new CollectStreamTableSink(targetAddress, targetPort, (TypeSerializer<Tuple2<Boolean, Row>>) serializer);
  }

  public List collectToList(Object table) throws Exception {
    return Lists.newArrayList(((Table) table).execute().collect());
  }

  public void startMultipleInsert(Object tblEnv, InterpreterContext context) throws Exception {
    StatementSet statementSet = ((TableEnvironment) tblEnv).createStatementSet();
    statementSetMap.put(context.getParagraphId(), statementSet);
  }

  public void addInsertStatement(String sql, Object tblEnv, InterpreterContext context) throws Exception {
    statementSetMap.get(context.getParagraphId()).addInsertSql(sql);
  }

  public boolean executeMultipleInsertInto(String jobName, Object tblEnv, InterpreterContext context) throws Exception {
    JobClient jobClient = statementSetMap.get(context.getParagraphId()).execute().getJobClient().get();
    while (!jobClient.getJobStatus().get().isTerminalState()) {
      LOGGER.debug("Wait for job to finish");
      Thread.sleep(1000 * 5);
    }
    if (jobClient.getJobStatus().get() == JobStatus.CANCELED) {
      context.out.write("Job is cancelled.\n");
      return false;
    }
    return true;
  }

  public boolean rowEquals(Object row1, Object row2) {
    Row r1 = (Row) row1;
    Row r2 = (Row) row2;
    r1.setKind(RowKind.INSERT);
    r2.setKind(RowKind.INSERT);
    return r1.equals(r2);
  }

  public Object fromDataSet(Object btenv, Object ds) {
    return Flink111ScalaShims.fromDataSet((BatchTableEnvironment) btenv, (DataSet) ds);
  }

  public Object toDataSet(Object btenv, Object table) {
    return Flink111ScalaShims.toDataSet((BatchTableEnvironment) btenv, (Table) table);
  }

  public void registerTableSink(Object stenv, String tableName, Object collectTableSink) {
    ((org.apache.flink.table.api.internal.TableEnvironmentInternal) stenv)
            .registerTableSinkInternal(tableName, (TableSink) collectTableSink);
  }

  public void registerTableFunction(Object btenv, String name, Object tableFunction) {
    ((StreamTableEnvironmentImpl) (btenv)).registerFunction(name, (TableFunction) tableFunction);
  }

  public void registerAggregateFunction(Object btenv, String name, Object aggregateFunction) {
    ((StreamTableEnvironmentImpl) (btenv)).registerFunction(name, (AggregateFunction) aggregateFunction);
  }

  public void registerTableAggregateFunction(Object btenv, String name, Object tableAggregateFunction) {
    ((StreamTableEnvironmentImpl) (btenv)).registerFunction(name, (TableAggregateFunction) tableAggregateFunction);
  }

  /**
   * Parse it via flink SqlParser first, then fallback to regular expression matching.
   *
   * @param tableEnv
   * @param stmt
   * @return
   */
  public Optional<SqlCommandParser.SqlCommandCall> parseSql(Object tableEnv, String stmt) {
    Parser sqlParser = ((TableEnvironmentInternal) tableEnv).getParser();
    SqlCommandParser.SqlCommandCall sqlCommandCall = null;
    try {
      // parse statement via regex matching first
      Optional<SqlCommandParser.SqlCommandCall> callOpt = parseByRegexMatching(stmt);
      if (callOpt.isPresent()) {
        sqlCommandCall = callOpt.get();
      } else {
        sqlCommandCall = parseBySqlParser(sqlParser, stmt);
      }
    } catch (Exception e) {
      return Optional.empty();
    }
    return Optional.of(sqlCommandCall);

  }

  private SqlCommandParser.SqlCommandCall parseBySqlParser(Parser sqlParser, String stmt) throws Exception {
    List<Operation> operations;
    try {
      operations = sqlParser.parse(stmt);
    } catch (Throwable e) {
      throw new Exception("Invalidate SQL statement.", e);
    }
    if (operations.size() != 1) {
      throw new Exception("Only single statement is supported now.");
    }

    final SqlCommandParser.SqlCommand cmd;
    String[] operands = new String[]{stmt};
    Operation operation = operations.get(0);
    if (operation instanceof CatalogSinkModifyOperation) {
      boolean overwrite = ((CatalogSinkModifyOperation) operation).isOverwrite();
      cmd = overwrite ? SqlCommandParser.SqlCommand.INSERT_OVERWRITE : SqlCommandParser.SqlCommand.INSERT_INTO;
    } else if (operation instanceof CreateTableOperation) {
      cmd = SqlCommandParser.SqlCommand.CREATE_TABLE;
    } else if (operation instanceof DropTableOperation) {
      cmd = SqlCommandParser.SqlCommand.DROP_TABLE;
    } else if (operation instanceof AlterTableOperation) {
      cmd = SqlCommandParser.SqlCommand.ALTER_TABLE;
    } else if (operation instanceof CreateViewOperation) {
      cmd = SqlCommandParser.SqlCommand.CREATE_VIEW;
    } else if (operation instanceof DropViewOperation) {
      cmd = SqlCommandParser.SqlCommand.DROP_VIEW;
    } else if (operation instanceof CreateDatabaseOperation) {
      cmd = SqlCommandParser.SqlCommand.CREATE_DATABASE;
    } else if (operation instanceof DropDatabaseOperation) {
      cmd = SqlCommandParser.SqlCommand.DROP_DATABASE;
    } else if (operation instanceof AlterDatabaseOperation) {
      cmd = SqlCommandParser.SqlCommand.ALTER_DATABASE;
    } else if (operation instanceof CreateCatalogOperation) {
      cmd = SqlCommandParser.SqlCommand.CREATE_CATALOG;
    } else if (operation instanceof DropCatalogOperation) {
      cmd = SqlCommandParser.SqlCommand.DROP_CATALOG;
    } else if (operation instanceof UseCatalogOperation) {
      cmd = SqlCommandParser.SqlCommand.USE_CATALOG;
      operands = new String[]{((UseCatalogOperation) operation).getCatalogName()};
    } else if (operation instanceof UseDatabaseOperation) {
      cmd = SqlCommandParser.SqlCommand.USE;
      operands = new String[]{((UseDatabaseOperation) operation).getDatabaseName()};
    } else if (operation instanceof ShowCatalogsOperation) {
      cmd = SqlCommandParser.SqlCommand.SHOW_CATALOGS;
      operands = new String[0];
    } else if (operation instanceof ShowDatabasesOperation) {
      cmd = SqlCommandParser.SqlCommand.SHOW_DATABASES;
      operands = new String[0];
    } else if (operation instanceof ShowTablesOperation) {
      cmd = SqlCommandParser.SqlCommand.SHOW_TABLES;
      operands = new String[0];
    } else if (operation instanceof ShowFunctionsOperation) {
      cmd = SqlCommandParser.SqlCommand.SHOW_FUNCTIONS;
      operands = new String[0];
    } else if (operation instanceof CreateCatalogFunctionOperation ||
            operation instanceof CreateTempSystemFunctionOperation) {
      cmd = SqlCommandParser.SqlCommand.CREATE_FUNCTION;
    } else if (operation instanceof DropCatalogFunctionOperation ||
            operation instanceof DropTempSystemFunctionOperation) {
      cmd = SqlCommandParser.SqlCommand.DROP_FUNCTION;
    } else if (operation instanceof AlterCatalogFunctionOperation) {
      cmd = SqlCommandParser.SqlCommand.ALTER_FUNCTION;
    } else if (operation instanceof ExplainOperation) {
      cmd = SqlCommandParser.SqlCommand.EXPLAIN;
    } else if (operation instanceof DescribeTableOperation) {
      cmd = SqlCommandParser.SqlCommand.DESCRIBE;
      operands = new String[]{((DescribeTableOperation) operation).getSqlIdentifier().asSerializableString()};
    } else if (operation instanceof QueryOperation) {
      cmd = SqlCommandParser.SqlCommand.SELECT;
    } else {
      throw new Exception("Unknown operation: " + operation.asSummaryString());
    }

    return new SqlCommandParser.SqlCommandCall(cmd, operands, stmt);
  }

  private static Optional<SqlCommandParser.SqlCommandCall> parseByRegexMatching(String stmt) {
    // parse statement via regex matching
    for (SqlCommandParser.SqlCommand cmd : SqlCommandParser.SqlCommand.values()) {
      if (cmd.pattern != null) {
        final Matcher matcher = cmd.pattern.matcher(stmt);
        if (matcher.matches()) {
          final String[] groups = new String[matcher.groupCount()];
          for (int i = 0; i < groups.length; i++) {
            groups[i] = matcher.group(i + 1);
          }
          return cmd.operandConverter.apply(groups)
                  .map((operands) -> {
                    String[] newOperands = operands;
                    if (cmd == SqlCommandParser.SqlCommand.EXPLAIN) {
                      // convert `explain xx` to `explain plan for xx`
                      // which can execute through executeSql method
                      newOperands = new String[]{"EXPLAIN PLAN FOR " + operands[0] + " " + operands[1]};
                    }
                    return new SqlCommandParser.SqlCommandCall(cmd, newOperands, stmt);
                  });
        }
      }
    }
    return Optional.empty();
  }

  public void executeSql(Object tableEnv, String sql) {
    ((TableEnvironment) tableEnv).executeSql(sql);
  }


  /**
   * Flink 1.11 bind CatalogManager with parser which make blink and flink could not share the same CatalogManager.
   * This is a workaround which always reset CatalogTableSchemaResolver before running any flink code.
   *
   * @param catalogManager
   * @param parserObject
   * @param environmentSetting
   */
  public void setCatalogManagerSchemaResolver(Object catalogManager,
                                              Object parserObject,
                                              Object environmentSetting) {
    ((CatalogManager) catalogManager).setCatalogTableSchemaResolver(
            new CatalogTableSchemaResolver((Parser) parserObject,
                    ((EnvironmentSettings) environmentSetting).isStreamingMode()));
  }

  public Object getCustomCli(Object cliFrontend, Object commandLine) {
    return ((CliFrontend) cliFrontend).validateAndGetActiveCommandLine((CommandLine) commandLine);
  }

  public Map extractTableConfigOptions() {
    Map<String, ConfigOption> configOptions = new HashMap<>();
    configOptions.putAll(extractConfigOptions(ExecutionConfigOptions.class));
    configOptions.putAll(extractConfigOptions(OptimizerConfigOptions.class));
    configOptions.putAll(extractConfigOptions(TableConfigOptions.class));
    return configOptions;
  }

  private Map<String, ConfigOption> extractConfigOptions(Class clazz) {
    Map<String, ConfigOption> configOptions = new HashMap();
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (field.getType().isAssignableFrom(ConfigOption.class)) {
        try {
          ConfigOption configOption = (ConfigOption) field.get(ConfigOption.class);
          configOptions.put(configOption.key(), configOption);
        } catch (Throwable e) {
          LOGGER.warn("Fail to get ConfigOption", e);
        }
      }
    }
    return configOptions;
  }

}
