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

import com.streamxhub.streamx.flink.core.{FlinkSqlValidationResult, FlinkSqlValidator}
import org.apache.flink.util.FileUtils
import org.scalatest.funsuite.AnyFunSuite

import java.io.File

// scalastyle:off println
class FlinkSqlValidationFunSuite extends AnyFunSuite {

  val sqlFilePath: String = new File("").getCanonicalPath + File.separator + "streamx-flink" + File.separator + "streamx-flink-shims" + File.separator + "streamx-flink-shims-test" + File.separator + "sqlFile" + File.separator

  def verify(sql: String)(func: FlinkSqlValidationResult => Unit): Unit = func(FlinkSqlValidator.verifySql(sql.stripMargin))


  test("validation") {
    val sql = FileUtils.readFileUtf8(new File(sqlFilePath + "sql_syntax.sql"))
    var allSuccess = true

    verify(sql) { r => {
      if (r.success == false) {
        allSuccess = false
        println(r.toString)
      }
    }
    }

    if (allSuccess) {
      println("All sql syntax check pass!")
    }
  }


}
