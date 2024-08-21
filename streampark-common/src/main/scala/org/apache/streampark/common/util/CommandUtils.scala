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

package org.apache.streampark.common.util

import org.apache.streampark.common.util.Implicits._

import java.io._
import java.lang.{Iterable => JavaIterable}
import java.util.Scanner
import java.util.function.Consumer

import scala.util.{Failure, Success, Try}

object CommandUtils extends Logger {

  @throws[Exception]
  def execute(command: String): (Int, String) = {
    Try {
      val buffer = new StringBuffer()
      val process: Process = Runtime.getRuntime.exec(command)
      val reader = new InputStreamReader(process.getInputStream)
      val scanner = new Scanner(reader)
      while (scanner.hasNextLine) {
        buffer.append(scanner.nextLine()).append("\n")
      }
      val code = waitFor(process)
      reader.close()
      scanner.close()
      code -> buffer.toString
    } match {
      case Success(v) => v
      case Failure(e) => throw e
    }
  }

  def execute(
      directory: String,
      commands: JavaIterable[String],
      consumer: Consumer[String]): Int = {
    Try {
      require(
        commands != null && commands.nonEmpty,
        "[StreamPark] CommandUtils.execute: commands must not be null.")
      logDebug(s"Command execute:\n${commands.mkString("\n")} ")

      // 1) init
      lazy val process = {
        val interpreters =
          if (Utils.isWindows) List("cmd", "/k") else List("/bin/bash")
        val builder = new ProcessBuilder(interpreters).redirectErrorStream(true)
        if (directory != null) {
          builder.directory(new File(directory))
        }
        builder.start
      }

      // 2) input
      def input(): Unit = {
        val out =
          new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream)), true)
        // scalastyle:off println
        commands.foreach(out.println)
        if (!commands.last.equalsIgnoreCase("exit")) {
          out.println("exit")
        }
        // scalastyle:on println
        out.close()
      }

      // 3) out
      def output(): Unit = {
        val scanner = new Scanner(process.getInputStream)
        while (scanner.hasNextLine) {
          consumer.accept(scanner.nextLine)
        }
        scanner.close()
      }

      input()
      output()
      waitFor(process)
    } match {
      case Success(code) => code
      case Failure(e) => throw e
    }
  }

  private[this] def waitFor(process: Process): Int = {
    val code = process.waitFor()
    process.getErrorStream.close()
    process.getInputStream.close()
    process.getOutputStream.close()
    process.destroy()
    code
  }

}
