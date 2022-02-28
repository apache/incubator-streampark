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


import com.google.gson.Gson

import scala.reflect.ClassTag

object JsonUtils extends Serializable {

  private val DATE_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss"

  private[this] lazy val gson = new Gson()
    .newBuilder()
    .setDateFormat(DATE_FORMAT_DEFAULT)
    .create()

  def read[T](json: String)(implicit tag: ClassTag[T]): T = gson.fromJson(json, tag.runtimeClass)

  def read[T](json: String, clazz: Class[T]): T = gson.fromJson(json, clazz)

  def write(obj: Any): String = gson.toJson(obj)

  implicit class Unmarshal(jsonStr: String) {
    def fromJson[T](implicit clazz: ClassTag[T]): T = gson.fromJson(jsonStr, clazz.runtimeClass)
  }

  implicit class Marshal(obj: AnyRef) {
    def toJson: String = write(obj)
  }
}

