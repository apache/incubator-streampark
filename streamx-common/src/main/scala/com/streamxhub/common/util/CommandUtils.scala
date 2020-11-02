/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.common.util

import scala.util.control.Breaks._
import java.io.BufferedReader
import java.io.InputStreamReader

import scala.util.{Failure, Success, Try}


object CommandUtils {

  def execute(command: String): String = {
    val buffer = new StringBuffer()
    Try {
      val process: Process = Runtime.getRuntime.exec(command)
      val reader: BufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream))
      breakable {
        while (true) {
          val line: String = reader.readLine()
          if (line == null) {
            break
          } else {
            buffer.append(line).append("\n")
          }
        }
      }
      if (process != null) {
        process.waitFor()
        process.getErrorStream.close()
        process.getInputStream.close()
        process.getOutputStream.close()
      }
      if (reader != null) {
        reader.close()
      }
    } match {
      case Success(value) =>
      case Failure(e) => e.printStackTrace()
    }
    buffer.toString
  }

}
