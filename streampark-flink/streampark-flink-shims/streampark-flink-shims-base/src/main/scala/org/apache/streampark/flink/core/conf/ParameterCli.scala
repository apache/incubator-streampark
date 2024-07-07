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
package org.apache.streampark.flink.core.conf

import org.apache.streampark.common.conf.ConfigKeys
import org.apache.streampark.common.conf.ConfigKeys.{KEY_FLINK_OPTION_PREFIX, KEY_FLINK_PROPERTY_PREFIX}
import org.apache.streampark.common.util.PropertiesUtils

import org.apache.commons.cli.{DefaultParser, Options}

import java.net.URLClassLoader

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object ParameterCli {

  private[this] val propertyPrefix = KEY_FLINK_PROPERTY_PREFIX
  private[this] val optionPrefix = KEY_FLINK_OPTION_PREFIX
  private[this] val optionMain = s"$propertyPrefix$$internal.application.main"

  lazy val flinkOptions: Options = FlinkRunOption.allOptions

  lazy val parser = new DefaultParser

  def main(args: Array[String]): Unit = print(read(args))

  def read(args: Array[String]): String = {
    args(0) match {
      case "--vmopt" =>
        // solved jdk1.8+ dynamic loading of resources to the classpath problem
        ClassLoader.getSystemClassLoader match {
          case c if c.isInstanceOf[URLClassLoader] => ""
          case _ =>
            "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED"
        }
      case action =>
        val conf = args(1)
        val map = Try {
          val extension = conf.split("\\.").last.toLowerCase
          extension match {
            case "yml" | "yaml" => PropertiesUtils.fromYamlFile(conf)
            case "conf" => PropertiesUtils.fromHoconFile(conf)
            case "properties" => PropertiesUtils.fromPropertiesFile(conf)
            case _ =>
              throw new IllegalArgumentException(
                "[StreamPark] Usage:flink.conf file error,must be (yml|conf|properties)")
          }
        } match {
          case Success(value) => value
          case _ => Map.empty[String, String]
        }
        val programArgs = args.drop(2)
        action match {
          case "--option" =>
            val option = getOption(map, programArgs)
            val buffer = new StringBuffer()
            Try {
              val line = parser.parse(flinkOptions, option, false)
              line.getOptions.foreach(x => {
                buffer.append(s" -${x.getOpt}")
                if (x.hasArg) {
                  buffer.append(s" ${x.getValue()}")
                }
              })
            } match {
              case Failure(exception) => exception.printStackTrace()
              case _ =>
            }
            map.getOrElse(optionMain, null) match {
              case null =>
              case mainClass => buffer.append(s" -c $mainClass")
            }
            buffer.toString.trim
          case "--property" =>
            val buffer = new StringBuffer()
            map
              .filter(x => x._1 != optionMain && x._1.startsWith(propertyPrefix) && x._2.nonEmpty)
              .foreach {
                x =>
                  val key = x._1.drop(propertyPrefix.length).trim
                  val value = x._2.trim
                  if (key == ConfigKeys.KEY_FLINK_APP_NAME) {
                    buffer.append(s" -D$key=${value.replace(" ", "_")}")
                  } else {
                    buffer.append(s" -D$key=$value")
                  }
              }
            buffer.toString.trim
          case "--name" =>
            map
              .getOrElse(propertyPrefix.concat(ConfigKeys.KEY_FLINK_APP_NAME), "")
              .trim match {
              case appName if appName.nonEmpty => appName
              case _ => ""
            }
          // is detached mode
          case "--detached" =>
            val option = getOption(map, programArgs)
            val line = parser.parse(FlinkRunOption.allOptions, option, false)
            val detached = line.hasOption(FlinkRunOption.DETACHED_OPTION.getOpt) || line.hasOption(
              FlinkRunOption.DETACHED_OPTION.getLongOpt)
            val mode = if (detached) "Detached" else "Attach"
            mode
          case _ => null
        }
    }
  }

  def getOption(map: Map[String, String], args: Array[String]): Array[String] = {
    val optionMap = new mutable.HashMap[String, Any]()
    map
      .filter(_._1.startsWith(optionPrefix))
      .filter(_._2.nonEmpty)
      .filter(x => {
        val key = x._1.drop(optionPrefix.length)
        // verify parameters is valid
        flinkOptions.hasOption(key)
      })
      .foreach(x => {
        Try(x._2.toBoolean).getOrElse(x._2) match {
          case b if b.isInstanceOf[Boolean] =>
            if (b.asInstanceOf[Boolean]) {
              optionMap += s"-${x._1.drop(optionPrefix.length)}".trim -> true
            }
          case v =>
            optionMap += s"-${x._1.drop(optionPrefix.length)}".trim -> v
        }
      })
    // parameters from the command line, which have a higher priority than the configuration file,
    // if they exist, will be overwritten
    args match {
      case Array() =>
      case array =>
        Try {
          val line = parser.parse(flinkOptions, array, false)
          line.getOptions.foreach(x => {
            if (x.hasArg) {
              optionMap += s"-${x.getLongOpt}".trim -> x.getValue()
            } else {
              optionMap += s"-${x.getLongOpt}".trim -> true
            }
          })
        } match {
          case Failure(e) => e.printStackTrace()
          case _ =>
        }
    }
    val array = new ArrayBuffer[String]
    optionMap.foreach(x => {
      array += x._1
      if (x._2.isInstanceOf[String]) {
        array += x._2.toString
      }
    })
    array.toArray
  }

}
