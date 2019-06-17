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

package com.streamxhub.spark.core.util

import java.io.{File, FileInputStream, IOException, InputStream, InputStreamReader}
import java.util.Properties

import org.apache.spark.SparkException
import org.yaml.snakeyaml.Yaml

import java.util.{LinkedHashMap => JavaLinkedMap}
import scala.collection.JavaConverters._
import scala.collection.Map
import scalaj.http._

/**
  *
  */
object Utils {

  /** Load properties present in the given file. */
  def getPropertiesFromYaml(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile, s"Properties file $file is not a normal file")
    val inputStream: InputStream = new FileInputStream(file)

    def doEach(prefix: String, k: String, v: Any, propMap: collection.mutable.Map[String, String]): Map[String, String] = {
      v match {
        case map: JavaLinkedMap[String, Any] =>
          map.asScala.flatMap(x => {
            prefix match {
              case "" => doEach(k, x._1, x._2, propMap)
              case other => doEach(s"$other.$k", x._1, x._2, propMap)
            }
          })
        case text =>
          val value = text match {
            case null => ""
            case other => other.toString
          }
          prefix match {
            case "" => propMap += k -> value.toString
            case other => propMap += s"$other.$k" -> value.toString
          }
          propMap
      }
    }

    try {
      val yaml = new Yaml().load(inputStream).asInstanceOf[java.util.Map[String, Map[String, Any]]].asScala
      val map = collection.mutable.Map[String, String]()
      yaml.flatMap(x => doEach("", x._1, x._2, map))
    } catch {
      case e: IOException => throw new SparkException(s"Failed when loading Spark properties from $filename", e)
    } finally {
      inputStream.close()
    }
  }

  /** Load properties present in the given file. */
  def getPropertiesFromFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile, s"Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), "UTF-8")
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().asScala.map(
        k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException =>
        throw new SparkException(s"Failed when loading Spark properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  def classForName(className: String): Class[_] = {
    Class.forName(className, true, Thread.currentThread().getContextClassLoader)
  }

  def httpPost(url: String, data: String, headers: Map[String, String] = Map.empty[String, String]): (Int, String) = {
    var req = Http(url).postData(data)
    headers.foreach { case (k, v) => req = req.header(k, v) }
    val res = req.asString
    (res.code, res.body)
  }

  def httpGet(url: String, param: Seq[(String, String)] = Seq.empty[(String, String)]): (Int, String) = {
    val req = Http(url).params(param)
    val res = req.asString
    (res.code, res.body)
  }


  def main(args: Array[String]): Unit = {

    println(Utils.httpGet("http://bigdata-dev:9200/app/heartbeat/application_1526125629402_7874/15000", Seq.empty[(String, String)]))
  }
}
