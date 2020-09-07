/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.repl.flink.interpreter

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

import com.streamxhub.repl.flink.interpreter.sql.{AppendStreamSqlJob, SingleRowStreamSqlJob, UpdateStreamSqlJob}
import org.apache.flink.api.scala.DataSet
import org.apache.flink.table.api.Table
import org.apache.flink.table.api.internal.TableImpl
import org.apache.flink.types.Row
import org.apache.flink.util.StringUtils
import org.apache.zeppelin.interpreter.{InterpreterContext, InterpreterHookRegistry, ResultMessages, ZeppelinContext}
import org.apache.zeppelin.tabledata.TableDataUtils

import scala.collection.JavaConverters._
import scala.collection.{JavaConversions, Seq}

class FlinkReplContext(val flinkInterpreter: FlinkScalaInterpreter,
                       val hooks2: InterpreterHookRegistry,
                       val maxResult2: Int) extends ZeppelinContext(hooks2, maxResult2) {

  private val SQL_INDEX = new AtomicInteger(0)
  private var currentSql: String = _

  private val interpreterClassMap = Map(
    "flink" -> "com.streamxhub.repl.flink.interpreter.FlinkInterpreter",
    "bsql" -> "com.streamxhub.repl.flink.interpreter.FlinkBatchSqlInterpreter",
    "ssql" -> "com.streamxhub.repl.flink.interpreter.FlinkStreamSqlInterpreter"
  )

  private val supportedClasses = Seq(classOf[DataSet[_]], classOf[Table])

  def setCurrentSql(sql: String): Unit = this.currentSql = sql

  override def getSupportedClasses: _root_.java.util.List[Class[_]] = JavaConversions.seqAsJavaList(supportedClasses)

  override def getInterpreterClassMap: _root_.java.util.Map[String, String] = JavaConversions.mapAsJavaMap(interpreterClassMap)

  private def showTable(columnsNames: Array[String], rows: Seq[Row]): String = {
    val builder = new java.lang.StringBuilder("%table ")
    builder.append(columnsNames.mkString("\t"))
    builder.append("\n")
    val isLargerThanMaxResult = rows.size > maxResult
    var displayRows = rows
    if (isLargerThanMaxResult) {
      displayRows = rows.take(maxResult)
    }
    for (row <- displayRows) {
      var i = 0;
      while (i < row.getArity) {
        // expand array if the column is array
        builder.append(TableDataUtils.normalizeColumn(StringUtils.arrayAwareToString(row.getField(i))))
        i += 1
        if (i != row.getArity) {
          builder.append("\t");
        }
      }
      builder.append("\n")
    }

    if (isLargerThanMaxResult) {
      builder.append("\n")
      builder.append(ResultMessages.getExceedsLimitRowsMessage(maxResult, "zeppelin.spark.maxResult"))
    }
    // append %text at the end, otherwise the following output will be put in table as well.
    builder.append("\n%text ")
    builder.toString

  }

  override def showData(obj: Any, maxResult: Int): String = {
    obj match {
      case ds: DataSet[_] =>
        val btenv = flinkInterpreter.getBatchTableEnvironment("flink") //.asInstanceOf[BatchTableEnvironment]
        val table = flinkInterpreter.getFlinkShims.fromDataSet(btenv, ds).asInstanceOf[Table]
        //btenv.fromDataSet(ds)
        val columnNames: Array[String] = table.getSchema.getFieldNames
        val dsRows: DataSet[Row] = flinkInterpreter.getFlinkShims.toDataSet(btenv, table).asInstanceOf[DataSet[Row]]
        //        btenv.toDataSet[Row](table)
        showTable(columnNames, dsRows.first(maxResult + 1).collect())
      case _ => obj match {
        case table: Table =>
          val rows = flinkInterpreter.getFlinkShims.collectToList(obj.asInstanceOf[TableImpl]).asInstanceOf[java.util.List[Row]].asScala
          val columnNames = table.getSchema.getFieldNames
          showTable(columnNames, rows)
        case _ =>
          obj.toString
      }
    }
  }

  def showFlinkTable(table: Table): String = {
    val columnNames: Array[String] = table.getSchema.getFieldNames
    val btenv = flinkInterpreter.getJavaBatchTableEnvironment("flink")
    val dsRows: DataSet[Row] = flinkInterpreter.getFlinkShims.toDataSet(btenv, table).asInstanceOf[DataSet[Row]]
    showTable(columnNames, dsRows.first(maxResult + 1).collect())
  }

  def showBlinkTable(table: Table): String = {
    val rows = flinkInterpreter.getFlinkShims.collectToList(table.asInstanceOf[TableImpl]).asInstanceOf[java.util.List[Row]].asScala
    val columnNames = table.getSchema.getFieldNames
    showTable(columnNames, rows)
  }

  def show(table: Table, streamType: String, configs: Map[String, String] = Map.empty): Unit = {
    val context = InterpreterContext.get()
    configs.foreach(e => context.getLocalProperties.put(e._1, e._2))
    val tableName = s"UnnamedTable_${context.getParagraphId.replace("-", "_")}_${SQL_INDEX.getAndIncrement()}"
    val streamJob = streamType.toLowerCase match {
      case "single" => new SingleRowStreamSqlJob(
        flinkInterpreter.getStreamExecutionEnvironment(),
        table.asInstanceOf[TableImpl].getTableEnvironment,
        flinkInterpreter.getJobManager,
        context,
        flinkInterpreter.getDefaultParallelism,
        flinkInterpreter.getFlinkShims
      )
      case "append" =>
        new AppendStreamSqlJob(
          flinkInterpreter.getStreamExecutionEnvironment(),
          table.asInstanceOf[TableImpl].getTableEnvironment,
          flinkInterpreter.getJobManager,
          context,
          flinkInterpreter.getDefaultParallelism,
          flinkInterpreter.getFlinkShims
        )
      case "update" =>
        new UpdateStreamSqlJob(
          flinkInterpreter.getStreamExecutionEnvironment(),
          table.asInstanceOf[TableImpl].getTableEnvironment,
          flinkInterpreter.getJobManager,
          context,
          flinkInterpreter.getDefaultParallelism,
          flinkInterpreter.getFlinkShims)
      case _ => throw new IOException(s"Unrecognized stream type:$streamType ")
    }
    streamJob.run(table, tableName)
  }

  /**
   * Called by python
   *
   * @param table
   * @param streamType
   * @param configs
   */
  def show(table: Table, streamType: String, configs: java.util.Map[String, String]): Unit = {
    show(table, streamType, JavaConversions.mapAsScalaMap(configs).toMap)
  }

}