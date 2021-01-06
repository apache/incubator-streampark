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
package com.streamxhub.flink.core.scala.util

import com.streamxhub.common.util.{DeflaterUtils, HdfsUtils, PropertiesUtils}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.runtime.state.FunctionInitializationContext

import java.io.File
import java.util.concurrent.TimeUnit

object FlinkUtils {

  def getUnionListState[R: TypeInformation](context: FunctionInitializationContext, descriptorName: String): ListState[R] = {
    context.getOperatorStateStore.getUnionListState(new ListStateDescriptor(descriptorName, implicitly[TypeInformation[R]].getTypeClass))
  }

  def getTimeUnit(time: String, default: (Int, TimeUnit) = (5, TimeUnit.SECONDS)): (Int, TimeUnit) = {
    val timeUnit = time match {
      case "" => null
      case x: String =>
        val num = x.replaceAll("\\s+|[a-z|A-Z]+$", "").toInt
        val unit = x.replaceAll("^[0-9]+|\\s+", "") match {
          case "" => null
          case "s" => TimeUnit.SECONDS
          case "m" | "min" => TimeUnit.MINUTES
          case "h" => TimeUnit.HOURS
          case "d" | "day" => TimeUnit.DAYS
          case _ => throw new IllegalArgumentException()
        }
        (num, unit)
    }
    timeUnit match {
      case null => default
      case other if other._2 == null => (other._1 / 1000, TimeUnit.SECONDS) //未带单位,值必须为毫秒,这里转成对应的秒...
      case other => other
    }
  }

  private[core] def readFlinkConf(config: String): Map[String, String] = {
    val extension = config.split("\\.").last.toLowerCase
    config match {
      case x if x.startsWith("yaml://") =>
        PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("prop://") =>
        PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(x.drop(7)))
      case x if x.startsWith("hdfs://") =>

        /**
         * 如果配置文件为hdfs方式,则需要用户将hdfs相关配置文件copy到resources下...
         */
        val text = HdfsUtils.read(x)
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesText(text)
          case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
        }
      case _ =>
        val configFile = new File(config)
        require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
        extension match {
          case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
          case "yml" | "yaml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
          case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
        }
    }
  }

}
