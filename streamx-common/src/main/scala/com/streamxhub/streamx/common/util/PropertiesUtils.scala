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

import org.yaml.snakeyaml.Yaml

import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Properties, Scanner, HashMap => JavaMap, LinkedHashMap => JavaLinkedMap}
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map => MutableMap}

/**
 * @author benjobs
 */
object PropertiesUtils extends Logger {


  def readFile(filename: String): String = {
    val file = new File(filename)
    require(file.exists(), s"[StreamX] readFile: file $file does not exist")
    require(file.isFile, s"[StreamX] readFile: file $file is not a normal file")
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
        map.flatMap(x => {
          prefix match {
            case "" => eachAppendYamlItem(k, x._1, x._2, proper)
            case other => eachAppendYamlItem(s"$other.$k", x._1, x._2, proper)
          }
        }).toMap
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

  def fromYamlText(text: String): Map[String, String] = {
    try {
      val map = MutableMap[String, String]()
      new Yaml()
        .load(text)
        .asInstanceOf[java.util.Map[String, Map[String, Any]]]
        .flatMap(x => eachAppendYamlItem("", x._1, x._2, map)).toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading conf error:", e)
    }
  }

  def fromPropertiesText(conf: String): Map[String, String] = {
    try {
      val properties = new Properties()
      properties.load(new StringReader(conf))
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading properties ", e)
    }
  }

  /** Load Yaml present in the given file. */
  def fromYamlFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"[StreamX] fromYamlFile: Yaml file $file does not exist")
    require(file.isFile, s"[StreamX] fromYamlFile: Yaml file $file is not a normal file")
    val inputStream: InputStream = new FileInputStream(file)
    try {
      val map = MutableMap[String, String]()
      new Yaml()
        .load(inputStream)
        .asInstanceOf[java.util.Map[String, Map[String, Any]]]
        .flatMap(x => eachAppendYamlItem("", x._1, x._2, map)).toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading properties from $filename", e)
    } finally {
      inputStream.close()
    }
  }

  /** Load properties present in the given file. */
  def fromPropertiesFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"[StreamX] fromPropertiesFile: Properties file $file does not exist")
    require(file.isFile, s"[StreamX] fromPropertiesFile: Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), "UTF-8")
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  /** Load Yaml present in the given file. */
  def fromYamlFile(inputStream: InputStream): Map[String, String] = {
    require(inputStream != null, s"[StreamX] fromYamlFile: Properties inputStream  must not be null")
    try {
      val map = MutableMap[String, String]()
      new Yaml()
        .load(inputStream)
        .asInstanceOf[java.util.Map[String, Map[String, Any]]]
        .flatMap(x => eachAppendYamlItem("", x._1, x._2, map)).toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"Failed when loading yaml from inputStream", e)
    } finally {
      inputStream.close()
    }
  }

  /** Load properties present in the given file. */
  def fromPropertiesFile(inputStream: InputStream): Map[String, String] = {
    require(inputStream != null, s"[StreamX] fromPropertiesFile: Properties inputStream  must not be null")
    try {
      val properties = new Properties()
      properties.load(inputStream)
      properties.stringPropertyNames().map(k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException => throw new IllegalArgumentException(s"[StreamX] Failed when loading properties from inputStream", e)
    }
  }


  /**
   *
   * @param file
   * @return
   */
  def loadFlinkConfYaml(file: File): JavaMap[String, String] = {
    require(file != null && file.exists() && file.isFile, "[StreamX] loadFlinkConfYaml: file must not be null")
    loadFlinkConfYaml(org.apache.commons.io.FileUtils.readFileToString(file))
  }

  def loadFlinkConfYaml(yaml: String): JavaMap[String, String] = {
    require(yaml != null && yaml.nonEmpty, "[StreamX] loadFlinkConfYaml: yaml must not be null")
    val flinkConf = new JavaMap[String, String]()
    val scanner: Scanner = new Scanner(yaml)
    val lineNo: AtomicInteger = new AtomicInteger(0)
    while (scanner.hasNextLine) {
      val line = scanner.nextLine()
      lineNo.incrementAndGet()
      // 1. check for comments
      val comments = line.split("#", 2)
      val conf = comments(0).trim
      // 2. get key and value
      if (conf.nonEmpty) {
        val kv = conf.split(": ", 2)
        // skip line with no valid key-value pair
        val key = kv(0).trim
        val value = kv(1).trim
        // sanity check
        if (key.nonEmpty && value.nonEmpty) {
          flinkConf += key -> value
        } else {
          logWarn(s"Error after splitting key and value in configuration ${lineNo.get()}: $line")
        }
      }
    }
    flinkConf
  }

}
