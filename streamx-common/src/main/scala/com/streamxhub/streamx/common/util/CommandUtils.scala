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
package com.streamxhub.streamx.common.util

import java.io._
import java.lang.{Iterable => JavaIterable}
import java.util.Scanner
import java.util.function.Consumer
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

object CommandUtils extends Logger {

  @throws[Exception] def execute(command: String): String = {
    Try {
      val buffer = new StringBuffer()
      val process: Process = Runtime.getRuntime.exec(command)
      val reader = new InputStreamReader(process.getInputStream)
      val scanner = new Scanner(reader)
      while (scanner.hasNextLine) {
        buffer.append(scanner.nextLine()).append("\n")
      }
      processClose(process)
      reader.close()
      scanner.close()
      buffer.toString
    } match {
      case Success(v) => v
      case Failure(e) => throw e
    }
  }

  def execute(commands: JavaIterable[String], consumer: Consumer[String]): Unit = {
    Try {
      require(commands != null && commands.nonEmpty, "[StreamX] CommandUtils.execute: commands must not be null.")
      logDebug(s"Command execute:\n${commands.mkString("\n")} ")
      val interpreters = if (Utils.isWindows) List("cmd", "/k") else List("/bin/bash")
      val process = new ProcessBuilder(interpreters).redirectErrorStream(true).start
      val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream)), true)
      commands.foreach(out.println)
      commands.last.toLowerCase.trim match {
        case "exit" =>
        case _ => out.println("exit")
      }
      val scanner = new Scanner(process.getInputStream)
      while (scanner.hasNextLine) {
        consumer.accept(scanner.nextLine)
      }
      processClose(process)
      scanner.close()
      out.close()
    } match {
      case Success(_) =>
      case Failure(e) => throw e
    }
  }

  private[this] def processClose(process: Process): Unit = {
    process.waitFor()
    process.getErrorStream.close()
    process.getInputStream.close()
    process.getOutputStream.close()
    process.destroy()
  }


}
