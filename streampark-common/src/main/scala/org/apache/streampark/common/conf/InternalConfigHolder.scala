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

package org.apache.streampark.common.conf

import org.apache.streampark.common.util.{Logger, SystemPropertyUtils}
import org.apache.streampark.common.util.Utils.StringCasts

import javax.annotation.{Nonnull, Nullable}

import java.util
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConversions._
import scala.language.postfixOps

/**
 * Thread-safe configuration storage containers. All configurations will be automatically
 * initialized from the spring configuration items of the same name.
 */
object InternalConfigHolder extends Logger {

  private val initialCapacity = 45

  /** configuration values storage (key -> value) */
  private val confData = new ConcurrentHashMap[String, Any](initialCapacity)

  /** configuration key options storage (key -> ConfigOption) */
  private val confOptions = new ConcurrentHashMap[String, InternalOption](initialCapacity)

  /** Initialize the ConfigHub. */
  {
    Seq(CommonConfig, K8sFlinkConfig)
  }

  /** Register the ConfigOption */
  private[conf] def register(@Nonnull conf: InternalOption): Unit = {
    confOptions.put(conf.key, conf)
    if (conf.defaultValue != null) {
      confData.put(conf.key, conf.defaultValue)
    }
  }

  /**
   * Get configuration value via ConfigOption.
   *
   * When using this api, the type must be explicitly declared and the relevant type will be
   * automatically converted to some extent. 1) in scala: val result: Long =
   * ConfigHub.get(K8sFlinkConfig.sglMetricTrackTaskTimeoutSec) 2) in java: Long result =
   * ConfigHub.get(K8sFlinkConfig.sglMetricTrackTaskTimeoutSec());
   *
   * @return
   *   return the defaultValue of ConfigOption when the value has not been overwritten.
   */
  @Nonnull
  def get[T](@Nonnull conf: InternalOption): T = {
    confData.get(conf.key) match {
      case null =>
        SystemPropertyUtils.get(conf.key) match {
          case v if v != null => v.cast[T](conf.classType)
          case _ => conf.defaultValue.asInstanceOf[T]
        }
      case v: T => v
    }
  }

  /**
   * Get configuration value via key.
   *
   * When using this api, the type must be explicitly declared and the relevant type will be
   * automatically converted to some extent. 1) in scala: val result: Long =
   * ConfigHub.get("streampark.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric") 2) in
   * java: Long result =
   * ConfigHub.get("streampark.flink-k8s.tracking.polling-task-timeout-sec.cluster-metric");
   *
   * @throws IllegalArgumentException
   *   when the key has not been registered to ConfigHub.
   * @return
   *   return the defaultValue of ConfigOption when the value has not been overwritten.
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
              case v if v != null => v.cast[T](config.classType)
              case _ =>
                throw new IllegalArgumentException(s"config key has not been registered: $key")
            }
          case conf: InternalOption => conf.defaultValue.asInstanceOf[T]
        }
      case v: T => v
    }
  }

  /**
   * Get registered ConfigOption by key.
   *
   * @return
   *   nullable
   */
  @Nullable
  def getConfig(key: String): InternalOption = {
    confOptions.get(key)
  }

  /** Get keys of all registered ConfigOption. */
  @Nonnull
  def keys(): util.Set[String] = {
    val map = new util.HashMap[String, InternalOption](confOptions.size())
    map.putAll(confOptions)
    map.keySet()
  }

  /**
   * Overwritten configuration value.
   *
   * @param conf
   *   should not be null.
   * @param value
   *   the type of value should be same as conf.classType.
   * @throws IllegalArgumentException
   *   when the conf has not been registered, or the value type is not same as conf.classType.
   */
  @throws[IllegalArgumentException]
  def set(@Nonnull conf: InternalOption, value: Any): Unit = {
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

  /** log the current configuration info. */
  def log(): Unit = {
    val configKeys = keys()
    logInfo(s"""registered configs:
               |ConfigHub collected configs: ${configKeys.size}
               |  ${configKeys
                .map(
                  key =>
                    s"$key = ${if (key.contains("password")) ConfigConst.DEFAULT_DATAMASK_STRING
                      else get(key)}")
                .mkString("\n  ")}""".stripMargin)
  }

}
