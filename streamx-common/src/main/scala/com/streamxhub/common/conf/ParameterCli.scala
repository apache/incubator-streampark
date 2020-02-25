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
package com.streamxhub.common.conf


import com.streamxhub.common.util.PropertiesUtils
import org.apache.commons.cli.DefaultParser

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object ParameterCli {

  private[conf] val resourcePrefix = "flink.deployment.resource."
  private[conf] val dynamicPrefix = "flink.deployment.dynamic."

  def main(args: Array[String]): Unit = {
    val action = args(0)
    val conf = args(1)
    val map = if (conf.endsWith(".properties")) {
      PropertiesUtils.fromPropertiesFile(conf)
    } else {
      PropertiesUtils.fromYamlFile(conf)
    }
    action match {
      case "--resource" =>
        val optionMap = new mutable.HashMap[String, Any]()
        map.filter(x => x._1.startsWith(resourcePrefix) && x._2.nonEmpty).foreach(x => {
          x._2 match {
            case "true" | "false" => if (x._2 == "true") optionMap += s"-${x._1.drop(resourcePrefix.length)}" -> true
            case v => optionMap += s"-${x._1.drop(resourcePrefix.length)}" -> v
          }
        })
        val parser = new DefaultParser
        //来自从命令行输入的参数,优先级比配置文件高,若存在则覆盖...
        args.drop(2) match {
          case Array() =>
          case array => {
            val line = parser.parse(FlinkOption.allOptions, array, false)
            line.getOptions.foreach(x => {
              if (x.hasArg) {
                optionMap += s"-${x.getLongOpt}" -> x.getValue()
              } else {
                optionMap += s"-${x.getLongOpt}" -> true
              }
            })
          }
        }
        val array = new ArrayBuffer[String]
        optionMap.foreach(x => {
          array += x._1
          if (x._2.isInstanceOf[String]) {
            array += x._2.toString
          }
        })
        val buffer = new StringBuffer()
        val line = parser.parse(FlinkOption.allOptions, array.toArray, false)
        line.getOptions.foreach(x => {
          buffer.append(s" -${x.getOpt}")
          if (x.hasArg) {
            buffer.append(s" ${x.getValue()}")
          }
        })
        println(buffer.toString.trim)
      case "--dynamic" =>
        val buffer = new StringBuffer()
        map.filter(x => x._1.startsWith(dynamicPrefix) && x._2.nonEmpty).foreach(x => buffer.append(s" -yD ${x._1.drop(resourcePrefix.length)}=${x._2}"))
        println(buffer.toString.trim)
      case "--name" =>
        map.getOrElse(ConfigConst.KEY_FLINK_APP_NAME, "").trim match {
          case yarnName if yarnName.nonEmpty => println(" -yarnname " + yarnName)
          case _ => println("")
        }
      case _ =>

    }
  }


}
