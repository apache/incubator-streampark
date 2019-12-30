package com.streamxhub.flink.core.conf

import java.io.{File, FilenameFilter}


/**
 * for shell run parameter...
 */
object ShellConfigReader {

  def main(args: Array[String]): Unit = {
    val url = Thread.currentThread().getContextClassLoader.getResource("./")
    val propFiles = new File(url.getFile).listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.matches(".*\\.properties$|.*\\.yml$")
    }).map(_.getName).mkString(" ")
    println(propFiles)
  }

}
