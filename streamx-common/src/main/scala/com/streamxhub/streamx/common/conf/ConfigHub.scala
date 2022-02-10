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

package com.streamxhub.streamx.common.conf

import com.streamxhub.streamx.common.util.{Logger, SystemPropertyUtils}

import java.util
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.{Nonnull, Nullable}
import scala.collection.JavaConversions._
import scala.language.postfixOps

/**
 * Thread-safe configuration storage containers.
 * All configurations will be automatically initialized from the spring
 * configuration items of the same name.
 *
 * @author Al-assad
 */
object ConfigHub extends Logger {

  private val initialCapacity = 45

  /**
   * configuration values storage (key -> value)
   */
  private val confData = new ConcurrentHashMap[String, Any](initialCapacity)

  /**
   * configuration key options storage (key -> ConfigOption)
   */
  private val confOptions = new ConcurrentHashMap[String, ConfigOption](initialCapacity)

  /**
   * Initialize the ConfigHub.
   */
  {
    Seq(CommonConfig, K8sFlinkConfig)
  }

  /**
   * Register the ConfigOption
   */
  private[conf] def register(@Nonnull conf: ConfigOption): Unit = {
    confOptions.put(conf.key, conf)
    confData.put(conf.key, conf.defaultValue)
  }

  /**
   * Get configuration value via ConfigOption.
   *
   * When using this api, the type must be explicitly declared and the relevant type will be
   * automatically converted to some extent.
   * 1) in scala:
   * val result: Long = ConfigHub.get(K8sFlinkConfig.sglMetricTrkTaskTimeoutSec)
   * 2) in java:
   * Long result = ConfigHub.get(K8sFlinkConfig.sglMetricTrkTaskTimeoutSec());
   *
   * @return return the defaultValue of ConfigOption when the value has not been overwritten.
   */
  @Nonnull
  def get[T](@Nonnull conf: ConfigOption): T = {
    confData.get(conf.key) match {
      case null =>
        SystemPropertyUtils.get(conf.key) match {
          case v if v != null => Converter.convert[T](v, conf.classType)
          case _ => conf.defaultValue.asInstanceOf[T]
        }
      case v: T => v
    }
  }

  /**
   * Get configuration value via key.
   *
   * When using this api, the type must be explicitly declared and the relevant type will be
   * automatically converted to some extent.
   * 1) in scala:
   * val result: Long = ConfigHub.get("streamx.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric")
   * 2) in java:
   * Long result = ConfigHub.get("streamx.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric");
   *
   * @throws IllegalArgumentException when the key has not been registered to ConfigHub.
   * @return return the defaultValue of ConfigOption when the value has not been overwritten.
   */
  @throws[IllegalArgumentException]
  @Nonnull
  def get[T](@Nonnull key: String): T = {
    confData.get(key) match {
      case null =>
        confOptions.get(key) match {
          case null =>
            val config = getConfig(key)
            SystemPropertyUtils.get(key) match {
              case v if v != null => Converter.convert[T](v, config.classType)
              case _ => throw new IllegalArgumentException(s"config key has not been registered: $key")
            }
          case conf: ConfigOption => conf.defaultValue.asInstanceOf[T]
        }
      case v: T => v
    }
  }

  /**
   * Get registered ConfigOption by key.
   *
   * @return nullable
   */
  @Nullable
  def getConfig(key: String): ConfigOption = {
    confOptions.get(key)
  }

  /**
   * Get keys of all registered ConfigOption.
   */
  @Nonnull
  def keys(): util.Set[String] = {
    val map = new util.HashMap[String, ConfigOption](confOptions.size())
    map.putAll(confOptions)
    map.keySet()
  }

  /**
   * Overwritten configuration value.
   *
   * @param conf  should not be null.
   * @param value the type of value should be same as conf.classType.
   * @throws IllegalArgumentException when the conf has not been registered,
   *                                  or the value type is not same as conf.classType.
   */
  @throws[IllegalArgumentException]
  def set(@Nonnull conf: ConfigOption, value: Any): Unit = {
    if (!confOptions.containsKey(conf.key)) {
      throw new IllegalArgumentException(s"config key has not been registered: $conf")
    }
    value match {
      case null => confData.remove(conf.key)
      case v if conf.classType != v.getClass =>
        throw new IllegalArgumentException(
          s"config value type is not match of ${conf.key}, required: ${conf.classType}, actual: ${v.getClass}")
      case v =>
        SystemPropertyUtils.set(conf.key, v.toString)
        confData.put(conf.key, v)
    }
  }

  /**
   * log the current configuration info.
   */
  def log(): Unit = {
    val configKeys = keys()
    logInfo(
      s"""registered configs:
         |ConfigHub collected configs: ${configKeys.size}
         |  ${configKeys.map(key => s"$key = ${get(key)}").mkString("\n  ")}""".stripMargin)
  }



}

/**
 * Configuration option.
 * All configurations will automatically initialized from the spring configuration items
 * of the same name.
 *
 * @param key          key of configuration that consistent with the spring config.
 * @param defaultValue default value of configuration that <b>should not be null</b>.
 * @param classType    the class type of value. <b>please use java class type</b>.
 * @param description  description of configuration.
 * @author Al-assad
 */
case class ConfigOption(key: String,
                        defaultValue: Any,
                        classType: Class[_],
                        description: String = "") {
  // register conf to ConfigHub
  ConfigHub.register(this)
}

object Converter {

  def convert[T](v: String, classType: Class[_]): T = {
    classType match {
      case c if c == classOf[java.lang.String] => v.asInstanceOf[T]
      case c if c == classOf[java.lang.Integer] => java.lang.Integer.valueOf(v).asInstanceOf[T]
      case c if c == classOf[java.lang.Long] => java.lang.Long.valueOf(v).asInstanceOf[T]
      case c if c == classOf[java.lang.Boolean] => java.lang.Boolean.valueOf(v).asInstanceOf[T]
      case c if c == classOf[java.lang.Float] => java.lang.Float.valueOf(v).asInstanceOf[T]
      case c if c == classOf[java.lang.Double] => java.lang.Double.valueOf(v).asInstanceOf[T]
      case _ =>
        throw new IllegalArgumentException(s"Unsupported type: $classType")
    }
  }
}


