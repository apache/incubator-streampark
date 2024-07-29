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

import org.apache.streampark.shaded.com.fasterxml.jackson.annotation.JsonInclude
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}

import java.text.SimpleDateFormat

import scala.reflect.ClassTag

object JsonUtils extends Serializable {

  private val mapper = new ObjectMapper()

  // ignore fields that exist in the json string and do not exist in the java obj
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  // ignore the empty bean to json error
  mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

  // disable the default conversion of the object's time to timesstamps
  mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

  // uniform date format with the `yyyy-MM-dd HH:mm:ss` style
  mapper.setDateFormat(new SimpleDateFormat(DateUtils.fullFormat))

  def read[T](obj: AnyRef, clazz: Class[T]): T = {
    obj match {
      case str: String => mapper.readValue(str, clazz)
      case _ => mapper.readValue(write(obj), clazz)
    }
  }

  def read[T](obj: AnyRef)(implicit classTag: ClassTag[T]): T =
    this.read(obj, classTag.runtimeClass).asInstanceOf[T]

  def write(obj: AnyRef): String = mapper.writeValueAsString(obj)

  implicit class Unmarshal(jsonStr: String) {
    def fromJson[T]()(implicit classTag: ClassTag[T]): T = read[T](jsonStr)
  }

  implicit class Marshal(obj: AnyRef) {
    def toJson: String = write(obj)
  }

}
