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
