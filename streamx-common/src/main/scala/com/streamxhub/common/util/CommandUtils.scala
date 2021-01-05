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
import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.util.Scanner
import java.util.function.Consumer
import scala.util.{Failure, Success, Try}
import java.util.{List => JavaList}
import scala.collection.JavaConversions._

object CommandUtils extends Logger {

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
      case Success(_) =>
      case Failure(e) => e.printStackTrace()
    }
    buffer.toString
  }

  def execute(commands: JavaList[String], consumer: Consumer[String]): Unit = {
    Try {
      require(commands != null && commands.nonEmpty)
      commands.last.toLowerCase.trim match {
        case "exit" =>
        case _ => commands.add("exit")
      }
      logInfo(s"[StreamX] Command execute:${commands.mkString("\n")} ")
      val process = Runtime.getRuntime.exec("/bin/bash", null, null)
      val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream)), true)
      commands.foreach(out.println)
      val scanner = new Scanner(process.getInputStream)
      while (scanner.hasNextLine) {
        consumer.accept(scanner.nextLine)
      }
      process.waitFor
      scanner.close()
      process.getErrorStream.close()
      process.getInputStream.close()
      process.getOutputStream.close()
      process.destroy()
    } match {
      case Success(_) =>
      case Failure(e) => throw e
    }
  }

}
