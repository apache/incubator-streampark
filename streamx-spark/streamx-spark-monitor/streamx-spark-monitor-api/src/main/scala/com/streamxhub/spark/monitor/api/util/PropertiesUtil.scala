package com.streamxhub.spark.monitor.api.util

import java.io._
import java.util.{Properties, Scanner}

import org.yaml.snakeyaml.Yaml

import java.util.{LinkedHashMap => JavaLinkedMap}
import scala.collection.JavaConverters._
import scala.collection.Map

/**
  *
  */
object PropertiesUtil {

  def getFileSource(filename: String): String = {
    val file = new File(filename)
    require(file.exists(), s"Yaml file $file does not exist")
    require(file.isFile, s"Yaml file $file is not a normal file")
    val scanner = new Scanner(file)
    val buffer = new StringBuilder
    while (scanner.hasNextLine) {
      buffer.append(scanner.nextLine()).append("\r\n")
    }
    scanner.close()
    buffer.toString()
  }

  private[this] def eachAppendYamlItem(prefix: String, k: String, v: Any, proper: collection.mutable.Map[String, String]): Map[String, String] = {
    v match {
      case map: JavaLinkedMap[String, Any] =>
        map.asScala.flatMap(x => {
          prefix match {
            case "" => eachAppendYamlItem(k, x._1, x._2, proper)
            case other => eachAppendYamlItem(s"$other.$k", x._1, x._2, proper)
          }
        })
      case text =>
        val value = text match {
          case null => ""
          case other => other.toString
        }
        prefix match {
          case "" => proper += k -> value.toString
          case other => proper += s"$other.$k" -> value.toString
        }
        proper
    }
  }

  def getPropertiesFromYamlText(text: String): java.util.Map[String, String] = {
    try {
      val map = collection.mutable.Map[String, String]()
      val yaml = new Yaml().load(text).asInstanceOf[java.util.Map[String, Map[String, Any]]].asScala
      yaml.flatMap(x => eachAppendYamlItem("", x._1, x._2, map)).asJava
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading conf error:", e)
    }
  }

  /** Load Yaml present in the given file. */
  def getPropertiesFromYaml(filename: String): java.util.Map[String, String]  = {
    val file = new File(filename)
    require(file.exists(), s"Yaml file $file does not exist")
    require(file.isFile, s"Yaml file $file is not a normal file")
    val inputStream: InputStream = new FileInputStream(file)
    try {
      val map = collection.mutable.Map[String, String]()
      val yaml = new Yaml().load(inputStream).asInstanceOf[java.util.Map[String, Map[String, Any]]].asScala
      yaml.flatMap(x => eachAppendYamlItem("", x._1, x._2, map)).asJava
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading Spark properties from $filename", e)
    } finally {
      inputStream.close()
    }
  }

  /** Load properties present in the given file. */
  def getPropertiesFromFile(filename: String): java.util.Map[String, String]  = {
    val file = new File(filename)
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile, s"Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), "UTF-8")
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().asScala.map(k => (k, properties.getProperty(k).trim)).toMap.asJava
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException(s"Failed when loading Spark properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  def classForName(className: String): Class[_] = {
    Class.forName(className, true, Thread.currentThread().getContextClassLoader)
  }


}
