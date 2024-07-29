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

import org.apache.streampark.common.util.Implicits._

import java.util.Properties

import scala.util.{Failure, Success, Try}

/**
 * @param key
 *   key of configuration that consistent with the spring config.
 * @param defaultValue
 *   default value of configuration that <b>should not be null</b>.
 * @param classType
 *   the class type of value. <b>please use java class type</b>.
 * @param required
 *   is required <b>
 * @param description
 *   description of configuration.
 * @param handle
 *   Processing function of special parameters
 */
case class ConfigOption[T](
    key: String,
    defaultValue: T = null,
    required: Boolean,
    classType: Class[_],
    description: String = "",
    handle: String => T = null)(implicit prefix: String = "", prop: Properties) {

  private[this] lazy val fullKey =
    if (prefix != null && prefix.nonEmpty) s"$prefix.$key" else key

  def get(): T = handle match {
    case null =>
      if (required) {
        prop.get(fullKey) match {
          case null => throw error("Is require")
          case v => v.toString.cast[T](classType)
        }
      } else {
        prop.getProperty(fullKey) match {
          case null => defaultValue
          case v => v.cast[T](classType)
        }
      }
    case _ =>
      if (required) {
        Try(handle(fullKey)) match {
          case Success(v) => v
          case Failure(e) => throw error(e.getMessage)
        }
      } else {
        Try(handle(fullKey)) match {
          case Success(v) => v
          case Failure(_) => defaultValue
        }
      }
  }

  def error(message: String): Exception = {
    new IllegalArgumentException(s"[StreamPark] config error: key:$fullKey, detail: $message")
  }

}
