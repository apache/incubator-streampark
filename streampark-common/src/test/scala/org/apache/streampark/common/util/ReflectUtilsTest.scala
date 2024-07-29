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

import org.scalatest.funsuite.AnyFunSuite

class ReflectUtilsTest extends AnyFunSuite {
  case class TestObject(value: String)

  test("getField should return the correct field") {
    val field = ReflectUtils.getField(classOf[TestObject], "value")
    assert(field.getName == "value")
    assert(field.getType == classOf[String])
  }

  test("getField should handle non-existent fields gracefully") {
    val field = ReflectUtils.getField(classOf[TestObject], "nonExistentField")
    assert(field == null)
  }

  test("getFieldValue should handle non-existent field gracefully") {
    val obj = new TestObject("test")
    val value = ReflectUtils.getFieldValue(obj, "nonExistentField")
    assert(value == null)
  }

  test("setFieldValue should throw IllegalArgumentException for non-existent field") {
    val obj = new TestObject("test")
    assertThrows[IllegalArgumentException] {
      ReflectUtils.setFieldValue(obj, "nonExistentField", "value")
    }
  }
}
