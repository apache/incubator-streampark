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

import com.typesafe.config.ConfigFactory
import org.apache.commons.lang3.StringUtils
import org.yaml.snakeyaml.Yaml

import javax.annotation.Nonnull

import java.io._
import java.util.{HashMap => JavaMap, LinkedHashMap => JavaLinkedMap, Properties, Scanner}
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

import scala.collection.JavaConverters._
import scala.collection.convert.ImplicitConversions._
import scala.collection.mutable
import scala.collection.mutable.{Map => MutableMap}

object PropertiesUtils extends Logger {

  private[this] lazy val PROPERTY_PATTERN = Pattern.compile("(.*?)=(.*?)")

  private[this] lazy val MULTI_PROPERTY_REGEXP = "-D(.*?)\\s*=\\s*[\\\"|'](.*)[\\\"|']"

  private[this] lazy val MULTI_PROPERTY_PATTERN = Pattern.compile(MULTI_PROPERTY_REGEXP)

  def readFile(filename: String): String = {
    val file = new File(filename)
    require(file.exists(), s"[StreamPark] readFile: file $file does not exist")
    require(file.isFile, s"[StreamPark] readFile: file $file is not a normal file")
    val scanner = new Scanner(file)
    val buffer = new mutable.StringBuilder
    while (scanner.hasNextLine) {
      buffer.append(scanner.nextLine()).append("\r\n")
    }
    scanner.close()
    buffer.toString()
  }

  private[this] def handleEachAppendYamlItem(
      prefix: String,
      k: String,
      v: Any,
      proper: collection.mutable.Map[String, String]): Map[String, String] = {
    v match {
      case map: JavaLinkedMap[String, Any] =>
        map
          .flatMap(
            x => {
              prefix match {
                case "" => handleEachAppendYamlItem(k, x._1, x._2, proper)
                case other => handleEachAppendYamlItem(s"$other.$k", x._1, x._2, proper)
              }
            })
          .toMap
      case text =>
        val value = text match {
          case null => ""
          case other => other.toString
        }
        prefix match {
          case "" => proper += k -> value
          case other => proper += s"$other.$k" -> value
        }
        proper.toMap
    }
  }

  def parseYamlText(text: String): Map[String, String] = {
    try {
      val map = MutableMap[String, String]()
      new Yaml()
        .load(text)
        .asInstanceOf[java.util.Map[String, Map[String, Any]]]
        .flatMap(x => handleEachAppendYamlItem("", x._1, x._2, map))
        .toMap
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException(s"Failed when loading conf error:", e)
    }
  }

  def parseHoconText(conf: String): Map[String, String] = {
    require(conf != null, s"[StreamPark] fromHoconText: Hocon content must not be null")
    try parseHoconByReader(new StringReader(conf))
    catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading Hocon ", e)
    }
  }

  def parsePropertiesText(conf: String): Map[String, String] = {
    try {
      val properties = new Properties()
      properties.load(new StringReader(conf))
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException(s"Failed when loading properties ", e)
    }
  }

  /** Load Yaml present in the given file. */
  def loadYamlFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"[StreamPark] fromYamlFile: Yaml file $file does not exist")
    require(file.isFile, s"[StreamPark] fromYamlFile: Yaml file $file is not a normal file")
    val inputStream: InputStream = new FileInputStream(file)
    loadYamlFile(inputStream)
  }

  def loadHoconFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"[StreamPark] fromHoconFile: file $file does not exist")
    val inputStream = new FileInputStream(file)
    loadHoconFile(inputStream)
  }

  /** Load properties present in the given file. */
  def loadPropertiesFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"[StreamPark] fromPropertiesFile: Properties file $file does not exist")
    require(
      file.isFile,
      s"[StreamPark] fromPropertiesFile: Properties file $file is not a normal file")
    val inputStream = new FileInputStream(file)
    loadPropertiesFile(inputStream)
  }

  /** Load Yaml present in the given file. */
  def loadYamlFile(inputStream: InputStream): Map[String, String] = {
    require(
      inputStream != null,
      s"[StreamPark] fromYamlFile: Properties inputStream  must not be null")
    try {
      val map = MutableMap[String, String]()
      new Yaml()
        .load(inputStream)
        .asInstanceOf[java.util.Map[String, Map[String, Any]]]
        .flatMap(x => handleEachAppendYamlItem("", x._1, x._2, map))
        .toMap
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException(s"Failed when loading yaml from inputStream", e)
    } finally {
      inputStream.close()
    }
  }

  def loadHoconFile(inputStream: InputStream): Map[String, String] = {
    require(inputStream != null, s"[StreamPark] fromHoconFile: Hocon inputStream  must not be null")
    try
      parseHoconByReader(new InputStreamReader(inputStream))
    catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading Hocon ", e)
    }
  }

  private[this] def parseHoconByReader(reader: Reader): Map[String, String] = {
    try {
      ConfigFactory
        .parseReader(reader)
        .entrySet()
        .map {
          x =>
            val k = x.getKey.trim.replaceAll("\"", "")
            val v = x.getValue.unwrapped().toString.trim
            k -> v
        }
        .toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading Hocon ", e)
    }
  }

  /** Load properties present in the given file. */
  def loadPropertiesFile(inputStream: InputStream): Map[String, String] = {
    require(
      inputStream != null,
      s"[StreamPark] fromPropertiesFile: Properties inputStream  must not be null")
    try {
      val properties = new Properties()
      properties.load(inputStream)
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException(
          s"[StreamPark] Failed when loading properties from inputStream",
          e)
    }
  }

  def convertYamlTextToJava(text: String): JavaMap[String, String] =
    new JavaMap[String, String](parseYamlText(text).asJava)

  def convertHoconTextToJava(text: String): JavaMap[String, String] =
    new JavaMap[String, String](parseHoconText(text).asJava)

  def convertPropertiesTextToJava(text: String): JavaMap[String, String] =
    new JavaMap[String, String](parsePropertiesText(text).asJava)

  def parseYamlFileToJava(filename: String): JavaMap[String, String] =
    new JavaMap[String, String](loadYamlFile(filename).asJava)

  def parseHoconFileToJava(filename: String): JavaMap[String, String] =
    new JavaMap[String, String](loadHoconFile(filename).asJava)

  def parsePropertiesFileToJava(filename: String): JavaMap[String, String] =
    new JavaMap[String, String](loadPropertiesFile(filename).asJava)

  def parseYamlFileToJava(inputStream: InputStream): JavaMap[String, String] =
    new JavaMap[String, String](loadYamlFile(inputStream).asJava)

  def parseHoconFileToJava(inputStream: InputStream): JavaMap[String, String] =
    new JavaMap[String, String](loadHoconFile(inputStream).asJava)

  def parsePropertiesFileToJava(inputStream: InputStream): JavaMap[String, String] =
    new JavaMap[String, String](loadPropertiesFile(inputStream).asJava)

  /**
   * @param file
   * @return
   */
  def loadFlinkConfYaml(file: File): JavaMap[String, String] = {
    require(
      file != null && file.exists() && file.isFile,
      "[StreamPark] loadFlinkConfYaml: file must not be null")
    loadFlinkConfYaml(org.apache.commons.io.FileUtils.readFileToString(file))
  }

  def loadFlinkConfYaml(yaml: String): JavaMap[String, String] = {
    require(yaml != null && yaml.nonEmpty, "[StreamPark] loadFlinkConfYaml: yaml must not be null")
    val flinkConf = new JavaMap[String, String]()
    val scanner: Scanner = new Scanner(yaml)
    val lineNo: AtomicInteger = new AtomicInteger(0)
    while (scanner.hasNextLine) {
      val line = scanner.nextLine()
      lineNo.incrementAndGet()
      // 1. check for comments
      // [FLINK-27299] flink parsing parameter bug fixed.
      val comments = line.split("^#|\\s+#", 2)
      val conf = comments(0).trim
      // 2. get key and value
      if (conf.nonEmpty) {
        val kv = conf.split(": ", 2)
        // skip line with no valid key-value pair
        if (kv.length == 2) {
          val key = kv(0).trim
          val value = kv(1).trim
          // sanity check
          if (key.nonEmpty && value.nonEmpty) {
            flinkConf += key -> value
          } else {
            logWarn(s"Error after splitting key and value in configuration ${lineNo.get()}: $line")
          }
        } else {
          logWarn(s"Error while trying to split key and value in configuration. $lineNo : $line")
        }
      }
    }
    flinkConf
  }

  /** extract flink configuration from application.properties */
  @Nonnull def extractDynamicProperties(properties: String): Map[String, String] = {
    if (StringUtils.isBlank(properties)) Map.empty[String, String]
    else {
      val map = mutable.Map[String, String]()
      val simple = properties.replaceAll(MULTI_PROPERTY_REGEXP, "")
      simple.split("\\s?-D") match {
        case d if Utils.notEmpty(d) =>
          d.foreach(
            x => {
              if (x.nonEmpty) {
                val p = PROPERTY_PATTERN.matcher(x.trim)
                if (p.matches) {
                  map += p.group(1).trim -> p.group(2).trim
                }
              }
            })
        case _ =>
      }
      val matcher = MULTI_PROPERTY_PATTERN.matcher(properties)
      while (matcher.find()) {
        val opts = matcher.group()
        val index = opts.indexOf("=")
        val key = opts.substring(2, index).trim
        val value = opts.substring(index + 1).trim.replaceAll("(^[\"|']|[\"|']$)", "")
        map += key -> value
      }
      map.toMap
    }
  }

  @Nonnull def extractDynamicPropertiesAsJava(properties: String): JavaMap[String, String] =
    new JavaMap[String, String](extractDynamicProperties(properties).asJava)

}
