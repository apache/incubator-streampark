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

import org.junit.jupiter.api.{Assertions, Test}
import org.junit.jupiter.api.Assertions.{assertDoesNotThrow, assertEquals, assertFalse, assertThrows, assertTrue}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

import java.io.IOException
import java.net.URL
import java.util

class UtilsTest extends AnyFunSuite {

  test("requiredNotNull should throw NullPointerException if argument is null") {
    val nullPointerException = intercept[NullPointerException] {
      Utils.requireNotNull(null, "object can't be null")
    }
    assert(nullPointerException.getMessage == "object can't be null")
  }

  test("requireNotEmpty should check if argument is not empty") {
    assert(!Utils.requireNotEmpty(null))
    assert(Utils.requireNotEmpty(Array(1)))
    assert(Utils.requireNotEmpty("string"))
    assert(Utils.requireNotEmpty(Seq("Seq")))
    assert(Utils.requireNotEmpty(Iterable("Iterable")))

    val arrayList = new util.ArrayList[String](16)
    arrayList.add("arrayList")
    assert(Utils.requireNotEmpty(arrayList))

    val hashMap = new util.HashMap[String, String](16)
    hashMap.put("hash", "map")
    assert(Utils.requireNotEmpty(hashMap))
    assert(Utils.requireNotEmpty())
  }

  test("required should throw IllegalArgumentException if condition is false") {
    val illegalArgumentException = intercept[IllegalArgumentException] {
      Utils.required(false)
    }
    assert(illegalArgumentException.getMessage == null)
  }

  test("requireCheckJarFile should throw IOException if JAR file path is invalid") {
    val jar: URL = new URL("http://host/file")
    val ioException = intercept[IOException] {
      Utils.requireCheckJarFile(jar)
    }
    assert(ioException.getMessage == s"JAR file path is invalid $jar")
  }

  test("checkHttpURL should return false for non-HTTP URL") {
    var httpUrl = "http://www.example.com"
    assert(Utils.checkHttpURL(httpUrl))
    var httpsUrl = "https://www.example.com"
    assert(Utils.checkHttpURL(httpsUrl))

    httpUrl = "http://local";
    assert(!Utils.checkHttpURL(httpUrl))
    httpsUrl = "https://local"
    assert(!Utils.checkHttpURL(httpsUrl))
  }
}
