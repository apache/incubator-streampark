package com.streamxhub.flink.core.conf

import java.io.{File, FilenameFilter}

import com.streamxhub.flink.core.util.PropertiesUtils


/**
 * for shell run parameter...
 */
object ShellConfigReader {

  def main(args: Array[String]): Unit = {

    args.head match {
      case "--which" =>
        val url = Thread.currentThread().getContextClassLoader.getResource("./")
        val propFiles = new File(url.getFile).listFiles(new FilenameFilter {
          override def accept(dir: File, name: String): Boolean = name.matches(".*\\.properties$|.*\\.yml$")
        }).map(_.getName).mkString(" ")
        println(propFiles)
      case "--conf" =>
        val conf = args(1)
        val url = Thread.currentThread().getContextClassLoader.getResource(s"./${conf}")
        val configArgs = url.getPath.split("\\.").last match {
          case "properties" => PropertiesUtils.fromPropertiesFile(url.getPath)
          case "yml" => PropertiesUtils.fromYamlFile(url.getPath)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,muse be properties or yml")
        }
        var runConf = ""
        configArgs.filter(_._1.startsWith("flink.deploy")).foreach(x => runConf = s" --${x._1} ${x._2} $runConf")
        println(runConf)
      case _ =>
    }

  }

}
