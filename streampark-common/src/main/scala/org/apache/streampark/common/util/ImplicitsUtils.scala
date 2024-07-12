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

import org.apache.streampark.common.util.Utils.close

import java.lang.{Boolean => JavaBool, Byte => JavaByte, Double => JavaDouble, Float => JavaFloat, Integer => JavaInt, Long => JavaLong, Short => JavaShort}

import scala.language.implicitConversions

object ImplicitsUtils {

  implicit class AutoCloseImplicits[T <: AutoCloseable](autoCloseable: T) {

    implicit def autoClose[R](func: T => R)(implicit excFunc: Throwable => R = null): R = {
      try {
        func(autoCloseable)
      } catch {
        case e: Throwable if excFunc != null => excFunc(e)
      } finally {
        close(autoCloseable)
      }
    }

  }

  implicit class StringImplicits(v: String) {
    def cast[T](classType: Class[_]): T = {
      classType match {
        case c if c == classOf[String] => v.asInstanceOf[T]
        case c if c == classOf[Byte] => v.toByte.asInstanceOf[T]
        case c if c == classOf[Int] => v.toInt.asInstanceOf[T]
        case c if c == classOf[Long] => v.toLong.asInstanceOf[T]
        case c if c == classOf[Float] => v.toFloat.asInstanceOf[T]
        case c if c == classOf[Double] => v.toDouble.asInstanceOf[T]
        case c if c == classOf[Short] => v.toShort.asInstanceOf[T]
        case c if c == classOf[Boolean] => v.toBoolean.asInstanceOf[T]
        case c if c == classOf[JavaByte] => v.toByte.asInstanceOf[T]
        case c if c == classOf[JavaInt] => JavaInt.valueOf(v).asInstanceOf[T]
        case c if c == classOf[JavaLong] => JavaLong.valueOf(v).asInstanceOf[T]
        case c if c == classOf[JavaFloat] =>
          JavaFloat.valueOf(v).asInstanceOf[T]
        case c if c == classOf[JavaDouble] =>
          JavaDouble.valueOf(v).asInstanceOf[T]
        case c if c == classOf[JavaShort] =>
          JavaShort.valueOf(v).asInstanceOf[T]
        case c if c == classOf[JavaBool] => JavaBool.valueOf(v).asInstanceOf[T]
        case _ =>
          throw new IllegalArgumentException(s"Unsupported type: $classType")
      }
    }
  }
}
