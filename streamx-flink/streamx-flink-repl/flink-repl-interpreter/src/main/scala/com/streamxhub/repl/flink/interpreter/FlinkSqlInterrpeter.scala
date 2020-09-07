package com.streamxhub.repl.flink.interpreter

import java.io.{FileInputStream, IOException}
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.{Map, Properties}

import com.streamxhub.repl.flink.shims.sql.{SqlCommand, SqlCommandCall, SqlCommandParser}
import com.streamxhub.repl.flink.util.SqlSplitter
import javax.annotation.Nullable
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.configuration.ConfigOption
import org.apache.flink.core.execution.{JobClient, JobListener}
import org.apache.flink.table.api.TableEnvironment
import org.apache.flink.table.api.config.ExecutionConfigOptions
import org.apache.zeppelin.interpreter.{Interpreter, InterpreterContext, InterpreterException, InterpreterResult}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._

abstract class FlinkSqlInterrpeter(properties: Properties) extends Interpreter(properties) {
  protected val LOGGER: Logger = LoggerFactory.getLogger(classOf[FlinkSqlInterrpeter])
  protected var flinkInterpreter: FlinkInterpreter = _
  protected var tbenv: TableEnvironment = _
  private var sqlCommandParser: SqlCommandParser = _
  private var sqlSplitter: SqlSplitter = _
  private var defaultSqlParallelism: Int = 0
  private val lock: ReentrantReadWriteLock.WriteLock = new ReentrantReadWriteLock().writeLock
  // all the available sql config options. see
  // https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/table/config.html
  private var tableConfigOptions: java.util.Map[String, ConfigOption[Any]] = _

  protected def isBatch: Boolean

  @throws[InterpreterException] override def open(): Unit = {
    sqlCommandParser = new SqlCommandParser(flinkInterpreter.getFlinkShims, tbenv)
    this.sqlSplitter = new SqlSplitter
    val jobListener = new JobListener() {
      override def onJobSubmitted(@Nullable jobClient: JobClient, @Nullable throwable: Throwable): Unit = {
        if (lock.isHeldByCurrentThread) {
          lock.unlock()
          LOGGER.info("UnLock JobSubmitLock")
        }
      }

      override def onJobExecuted(@Nullable jobExecutionResult: JobExecutionResult, @Nullable throwable: Throwable): Unit = {}
    }
    flinkInterpreter.getExecutionEnvironment.getJavaEnv.registerJobListener(jobListener)
    flinkInterpreter.getStreamExecutionEnvironment.getJavaEnv.registerJobListener(jobListener)
    this.defaultSqlParallelism = flinkInterpreter.getDefaultSqlParallelism
    this.tableConfigOptions = flinkInterpreter.getFlinkShims.extractTableConfigOptions.asInstanceOf[Map[String, ConfigOption[Any]]]
  }

  @throws[InterpreterException] override def interpret(st: String, context: InterpreterContext): InterpreterResult = {
    LOGGER.debug("Interpret code: " + st)
    flinkInterpreter.getZeppelinContext.setInterpreterContext(context)
    flinkInterpreter.getZeppelinContext.setNoteGui(context.getNoteGui)
    flinkInterpreter.getZeppelinContext.setGui(context.getGui)
    // set ClassLoader of current Thread to be the ClassLoader of Flink scala-shell,
    // otherwise codegen will fail to find classes defined in scala-shell
    val originClassLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(flinkInterpreter.getFlinkScalaShellLoader)
      flinkInterpreter.createPlannerAgain()
      flinkInterpreter.setParallelismIfNecessary(context)
      flinkInterpreter.setSavepointIfNecessary(context)
      runSqlList(st, context)
    } finally Thread.currentThread.setContextClassLoader(originClassLoader)
  }

  private def runSqlList(st: String, context: InterpreterContext): InterpreterResult = {
    try {
      val runAsOne = context.getStringLocalProperty("runAsOne", "false").toBoolean
      val sqls = sqlSplitter.splitSql(st)
      var isFirstInsert = true
      for (sql <- sqls) {
        val sqlCommand = sqlCommandParser.parse(sql)
        if (!sqlCommand.isPresent) {
          try {
            context.out.write("%text Invalid Sql statement: " + sql + "\n")
            context.out.write(flinkInterpreter.getFlinkShims.sqlHelp)
          } catch {
            case e: IOException =>
              return new InterpreterResult(InterpreterResult.Code.ERROR, e.toString)
          }
          return new InterpreterResult(InterpreterResult.Code.ERROR)
        }
        try {
          if ((sqlCommand.get.command eq SqlCommand.INSERT_INTO) || (sqlCommand.get.command eq SqlCommand.INSERT_OVERWRITE)) if (isFirstInsert && runAsOne) {
            flinkInterpreter.getFlinkShims.startMultipleInsert(tbenv, context)
            isFirstInsert = false
          }
          callCommand(sqlCommand.get, context)
          context.out.flush()
        } catch {
          case e: Throwable =>
            LOGGER.error("Fail to run sql:" + sql, e)
            try context.out.write("%text Fail to run sql command: " + sql + "\n" + ExceptionUtils.getStackTrace(e) + "\n")
            catch {
              case ex: IOException =>
                LOGGER.warn("Unexpected exception:", ex)
                return new InterpreterResult(InterpreterResult.Code.ERROR, ExceptionUtils.getStackTrace(e))
            }
            return new InterpreterResult(InterpreterResult.Code.ERROR)
        }
      }
      if (runAsOne) try {
        lock.lock()
        val jobName = context.getStringLocalProperty("jobName", st)
        if (flinkInterpreter.getFlinkShims.executeMultipleInsertInto(jobName, this.tbenv, context)) context.out.write("Insertion successfully.\n")
      } catch {
        case e: Exception =>
          LOGGER.error("Fail to execute sql as one job", e)
          return new InterpreterResult(InterpreterResult.Code.ERROR, ExceptionUtils.getStackTrace(e))
      } finally if (lock.isHeldByCurrentThread) lock.unlock()
    } catch {
      case e: Exception =>
        LOGGER.error("Fail to execute sql", e)
        return new InterpreterResult(InterpreterResult.Code.ERROR, ExceptionUtils.getStackTrace(e))
    } finally {
      // reset parallelism
      this.tbenv.getConfig.getConfiguration.set[java.lang.Integer](ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM, defaultSqlParallelism)
      // reset table config
      tableConfigOptions.values.filter(_.defaultValue() != null).foreach(x => {
        this.tbenv.getConfig.getConfiguration.set(x, x.defaultValue)
      })
      this.tbenv.getConfig.getConfiguration.addAll(flinkInterpreter.getFlinkConfiguration)
    }
    new InterpreterResult(InterpreterResult.Code.SUCCESS)
  }

  @throws[Exception] private def callCommand(cmdCall: SqlCommandCall, context: InterpreterContext): Unit = {
    cmdCall.command match {
      case SqlCommand.HELP => callHelp(context)
      case SqlCommand.SHOW_CATALOGS => callShowCatalogs(context)
      case SqlCommand.SHOW_DATABASES => callShowDatabases(context)
      case SqlCommand.SHOW_TABLES => callShowTables(context)
      case SqlCommand.SOURCE => callSource(cmdCall.operands(0), context)
      case SqlCommand.CREATE_FUNCTION => callCreateFunction(cmdCall.operands(0), context)
      case SqlCommand.DROP_FUNCTION => callDropFunction(cmdCall.operands(0), context)
      case SqlCommand.ALTER_FUNCTION => callAlterFunction(cmdCall.operands(0), context)
      case SqlCommand.SHOW_FUNCTIONS => callShowFunctions(context)
      case SqlCommand.SHOW_MODULES => callShowModules(context)
      case SqlCommand.USE_CATALOG => callUseCatalog(cmdCall.operands(0), context)
      case SqlCommand.USE => callUseDatabase(cmdCall.operands(0), context)
      case SqlCommand.CREATE_CATALOG => callCreateCatalog(cmdCall.operands(0), context)
      case SqlCommand.DROP_CATALOG => callDropCatalog(cmdCall.operands(0), context)
      case SqlCommand.DESC =>
      case SqlCommand.DESCRIBE => callDescribe(cmdCall.operands(0), context)
      case SqlCommand.EXPLAIN => callExplain(cmdCall.operands(0), context)
      case SqlCommand.SELECT => callSelect(cmdCall.operands(0), context)
      case SqlCommand.SET => callSet(cmdCall.operands(0), cmdCall.operands(1), context)
      case SqlCommand.INSERT_INTO =>
      case SqlCommand.INSERT_OVERWRITE => callInsertInto(cmdCall.operands(0), context)
      case SqlCommand.CREATE_TABLE => callCreateTable(cmdCall.operands(0), context)
      case SqlCommand.DROP_TABLE => callDropTable(cmdCall.operands(0), context)
      case SqlCommand.CREATE_VIEW => callCreateView(cmdCall.operands(0), cmdCall.operands(1), context)
      case SqlCommand.DROP_VIEW => callDropView(cmdCall.operands(0), context)
      case SqlCommand.CREATE_DATABASE => callCreateDatabase(cmdCall.operands(0), context)
      case SqlCommand.DROP_DATABASE => callDropDatabase(cmdCall.operands(0), context)
      case SqlCommand.ALTER_DATABASE => callAlterDatabase(cmdCall.operands(0), context)
      case SqlCommand.ALTER_TABLE => callAlterTable(cmdCall.operands(0), context)
      case _ => throw new Exception("Unsupported command: " + cmdCall.command)
    }
  }

  @throws[IOException] private def callAlterTable(sql: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      this.tbenv.sqlUpdate(sql)
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
    context.out.write("Table has been modified.\n")
  }

  @throws[IOException] private def callAlterDatabase(sql: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      this.tbenv.sqlUpdate(sql)
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
    context.out.write("Database has been modified.\n")
  }

  @throws[IOException] private def callDropDatabase(sql: String, context: InterpreterContext): Unit = {
    try this.tbenv.sqlUpdate(sql) finally if (lock.isHeldByCurrentThread) lock.unlock()
    context.out.write("Database has been dropped.\n")
  }

  @throws[IOException] private def callCreateDatabase(sql: String, context: InterpreterContext): Unit = {
    try this.tbenv.sqlUpdate(sql)  finally if (lock.isHeldByCurrentThread) lock.unlock()
    context.out.write("Database has been created.\n")
  }

  @throws[IOException]
  private def callDropView(view: String, context: InterpreterContext): Unit = {
    this.tbenv.dropTemporaryView(view)
    context.out.write("View has been dropped.\n")
  }

  @throws[IOException] private def callCreateView(name: String, query: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      this.tbenv.createTemporaryView(name, tbenv.sqlQuery(query))
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
    context.out.write("View has been created.\n")
  }

  @throws[IOException] private def callCreateTable(sql: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      this.tbenv.sqlUpdate(sql)
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
    context.out.write("Table has been created.\n")
  }

  @throws[IOException] private def callDropTable(sql: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      this.tbenv.sqlUpdate(sql)
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
    context.out.write("Table has been dropped.\n")
  }

  @throws[IOException] private def callUseCatalog(catalog: String, context: InterpreterContext): Unit = {
    this.tbenv.useCatalog(catalog)
  }

  @throws[IOException]
  private def callCreateCatalog(sql: String, context: InterpreterContext): Unit = {
    flinkInterpreter.getFlinkShims.executeSql(tbenv, sql)
    context.out.write("Catalog has been created.\n")
  }

  @throws[IOException] private def callDropCatalog(sql: String, context: InterpreterContext): Unit = {
    flinkInterpreter.getFlinkShims.executeSql(tbenv, sql)
    context.out.write("Catalog has been dropped.\n")
  }

  @throws[IOException]
  private def callShowModules(context: InterpreterContext): Unit = {
    val modules = this.tbenv.listModules
    context.out.write("%table module\n" + StringUtils.join(modules, "\n") + "\n")
  }

  @throws[IOException]
  private def callHelp(context: InterpreterContext): Unit = {
    context.out.write(flinkInterpreter.getFlinkShims.sqlHelp)
  }

  @throws[IOException]
  private def callShowCatalogs(context: InterpreterContext): Unit = {
    val catalogs = this.tbenv.listCatalogs
    context.out.write("%table catalog\n" + StringUtils.join(catalogs, "\n") + "\n")
  }

  @throws[IOException]
  private def callShowDatabases(context: InterpreterContext): Unit = {
    val databases = this.tbenv.listDatabases
    context.out.write("%table database\n" + StringUtils.join(databases, "\n") + "\n")
  }

  @throws[IOException]
  private def callShowTables(context: InterpreterContext): Unit = {
    val tables = this.tbenv.listTables.filter(!_.startsWith("UnnamedTable"))
    context.out.write("%table table\n" + StringUtils.join(tables, "\n") + "\n")
  }

  @throws[IOException]
  private def callSource(sqlFile: String, context: InterpreterContext): Unit = {
    val sql = IOUtils.toString(new FileInputStream(sqlFile))
    runSqlList(sql, context)
  }

  @throws[IOException]
  private def callCreateFunction(sql: String, context: InterpreterContext): Unit = {
    flinkInterpreter.getFlinkShims.executeSql(tbenv, sql)
    context.out.write("Function has been created.\n")
  }

  @throws[IOException]
  private def callDropFunction(sql: String, context: InterpreterContext): Unit = {
    flinkInterpreter.getFlinkShims.executeSql(tbenv, sql)
    context.out.write("Function has been dropped.\n")
  }

  @throws[IOException]
  private def callAlterFunction(sql: String, context: InterpreterContext): Unit = {
    flinkInterpreter.getFlinkShims.executeSql(tbenv, sql)
    context.out.write("Function has been modified.\n")
  }

  @throws[IOException]
  private def callShowFunctions(context: InterpreterContext): Unit = {
    val functions = this.tbenv.listUserDefinedFunctions
    context.out.write("%table function\n" + StringUtils.join(functions, "\n") + "\n")
  }

  @throws[IOException]
  private def callUseDatabase(databaseName: String, context: InterpreterContext): Unit = {
    tbenv.useDatabase(databaseName)
  }

  @throws[IOException]
  private def callDescribe(name: String, context: InterpreterContext): Unit = {
    val schema = tbenv.scan(name).getSchema
    val builder = new StringBuilder
    builder.append("Column\tType\n")
    for (i <- 0 until schema.getFieldCount) {
      builder.append(schema.getFieldName(i).get + "\t" + schema.getFieldDataType(i).get + "\n")
    }
    context.out.write("%table\n" + builder.toString)
  }

  @throws[IOException]
  private def callExplain(sql: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      val table = this.tbenv.sqlQuery(sql)
      context.out.write(this.tbenv.explain(table) + "\n")
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
  }

  @throws[IOException]
  def callSelect(sql: String, context: InterpreterContext): Unit = {
    try {
      lock.lock()
      callInnerSelect(sql, context)
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
  }

  @throws[IOException]
  def callInnerSelect(sql: String, context: InterpreterContext): Unit

  @throws[IOException]
  def callSet(key: String, value: String, context: InterpreterContext): Unit = {
    if (!tableConfigOptions.containsKey(key)) {
      throw new IOException(key +
        " is not a valid table/sql config, please check link: " +
        "https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/table/config.html")
    }
    this.tbenv.getConfig.getConfiguration.setString(key, value)
  }

  @throws[IOException]
  def callInsertInto(sql: String, context: InterpreterContext): Unit = {
    if (!isBatch) context.getLocalProperties.put("flink.streaming.insert_into", "true")
    try {
      lock.lock()
      val runAsOne = context.getStringLocalProperty("runAsOne", "false").toBoolean
      if (runAsOne) {
        flinkInterpreter.getFlinkShims.addInsertStatement(sql, this.tbenv, context)
      } else {
        this.tbenv.sqlUpdate(sql)
        val jobName = context.getStringLocalProperty("jobName", sql)
        this.tbenv.execute(jobName)
        context.out.write("Insertion successfully.\n")
      }
    } catch {
      case e: Exception => throw new IOException(e)
    } finally if (lock.isHeldByCurrentThread) {
      lock.unlock()
    }
  }

  @throws[InterpreterException]
  override def cancel(context: InterpreterContext): Unit = {
    this.flinkInterpreter.cancel(context)
  }

  override def close(): Unit

  override def getFormType: Interpreter.FormType

  override def getProgress(interpreterContext: InterpreterContext): Int
}