/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.flink.core.test

import com.streamxhub.streamx.common.conf.ConfigConst.KEY_FLINK_SQL
import com.streamxhub.streamx.common.util.DeflaterUtils
import com.streamxhub.streamx.flink.core.{FlinkSqlExecutor, FlinkTableInitializer, StreamTableContext}
import org.apache.flink.util.FileUtils
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import scala.collection.mutable.ArrayBuffer

// scalastyle:off println
class FlinkSqlExecuteFunSuite extends AnyFunSuite {

  val sqlFilePath: String = new File("").getCanonicalPath + File.separator + "streamx-flink" + File.separator + "streamx-flink-shims" + File.separator + "streamx-flink-shims-test" + File.separator + "sqlFile" + File.separator

  def execute(sql: String)(implicit func: String => Unit): Unit = {
    val args = ArrayBuffer(KEY_FLINK_SQL("--"), DeflaterUtils.zipString(sql.stripMargin))
    val context = new StreamTableContext(FlinkTableInitializer.initStreamTable(args.toArray, null, null))
    FlinkSqlExecutor.executeSql(KEY_FLINK_SQL(), context.parameter, context)
  }


  test("execute") {
    val sql = FileUtils.readFileUtf8(new File(sqlFilePath + "sql_execute.sql"))

    execute(sql) { r =>
      println(r)
    }
  }

}
