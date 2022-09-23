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

package org.apache.streampark.flink.cli

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.ExecutionOptions
import org.apache.streampark.common.conf.ConfigConst.{KEY_APP_CONF, KEY_FLINK_SQL, KEY_FLINK_TABLE_MODE}
import org.apache.streampark.common.util.{DeflaterUtils, PropertiesUtils}
import org.apache.streampark.flink.core.{SqlCommand, SqlCommandParser}
import org.apache.streampark.flink.core.scala.{FlinkStreamTable, FlinkTable}

import scala.language.implicitConversions
import scala.util.Try

object SqlClient extends App {

  val parameterTool = ParameterTool.fromArgs(args)

  val flinkSql = {
    val sql = parameterTool.get(KEY_FLINK_SQL())
    require(sql != null && sql.trim.nonEmpty, "Usage: flink sql cannot be null")
    Try(DeflaterUtils.unzipString(sql)).getOrElse(throw new IllegalArgumentException("Usage: flink sql is invalid or null, please check"))
  }

  val sets = SqlCommandParser.parseSQL(flinkSql).filter(_.command == SqlCommand.SET)

  val mode = sets.find(_.operands.head == ExecutionOptions.RUNTIME_MODE.key()) match {
    case Some(e) => e.operands(1)
    case None =>
      val appConf = parameterTool.get(KEY_APP_CONF(), null)
      val defaultMode = "streaming"
      if (appConf == null) defaultMode else {
        val parameter = PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(appConf.drop(7)))
        parameter.getOrElse(KEY_FLINK_TABLE_MODE, defaultMode)
      }
  }

  mode match {
    case "batch" => BatchSqlApp.main(args)
    case "streaming" => StreamSqlApp.main(args)
    case _ => throw new IllegalArgumentException("Usage: runtime execution-mode invalid, optional [streaming|batch]")
  }

  private[this] object BatchSqlApp extends FlinkTable {
    override def handle(): Unit = context.sql()
  }

  private[this] object StreamSqlApp extends FlinkStreamTable {
    override def handle(): Unit = context.sql()
  }

}
