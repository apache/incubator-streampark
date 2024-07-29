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

import org.apache.streampark.common.util.Implicits._

import org.apache.commons.lang3.StringUtils

import java.lang.annotation.Annotation
import java.lang.reflect.{Field, Method, Modifier}
import java.util.Objects

import scala.util.{Failure, Success, Try}

object ReflectUtils extends Logger {

  /**
   * Get field.
   *
   * @param beanClass
   *   the bean class
   * @param name
   *   the name
   * @return
   *   the field
   * @throws SecurityException
   *   the security exception
   */
  @throws[SecurityException]
  def getField(beanClass: Class[_], name: String): Field = {
    Try(
      beanClass.getDeclaredFields
        .filter(f => Objects.equals(name, f.getName))
        .head)
      .getOrElse(null)
  }

  def getFieldValue(obj: Any, fieldName: String): Any = {
    val field = getField(obj.getClass, fieldName)
    getFieldValue(obj, field)
  }

  def getFieldValue(obj: Any, field: Field): Any = {
    if (obj == null || field == null) {
      null
    } else {
      field.setAccessible(true)
      field.get(obj) match {
        case Success(v) => v
        case Failure(e) => throw e
      }
    }
  }

  def setFieldValue(obj: Any, fieldName: String, value: Any): Unit = {
    val field = getAccessibleField(obj, fieldName)
    if (field == null) {
      throw new IllegalArgumentException(
        "Could not find field [" + fieldName + "] on target [" + obj + "]")
    }
    try field.set(obj, value)
    catch {
      case e: IllegalAccessException =>
        logError("Failed to assign to the element.", e)
        throw new Exception(e.getMessage)
    }
  }

  private def getAccessibleField(obj: Any, fieldName: String): Field = {
    require(obj != null, "object can't be null")
    require(StringUtils.isNotBlank(fieldName), "fieldName can't be blank")
    var superClass = obj.getClass
    while (superClass ne classOf[Any]) {
      try {
        val field = superClass.getDeclaredField(fieldName)
        makeAccessible(field)
        return field
      } catch {
        case _: NoSuchFieldException =>
      }
      superClass = superClass.getSuperclass
    }
    null
  }

  private def makeAccessible(field: Field): Unit = {
    if ((!Modifier.isPublic(field.getModifiers)
        || !Modifier.isPublic(field.getDeclaringClass.getModifiers)
        || Modifier.isFinal(field.getModifiers)) && !field.isAccessible) {
      field.setAccessible(true)
    }
  }

  def getMethodsByAnnotation(beanClass: Class[_], annotClazz: Class[_ <: Annotation]): JavaList[Method] = {
    beanClass.getDeclaredMethods.filter(_.getDeclaredAnnotation(annotClazz) != null).toList
  }

}
