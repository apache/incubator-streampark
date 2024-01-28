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

import java.io.IOException
import java.net.URL
import java.util

class UtilsTest {
  @Test def requiredNotNullTest(): Unit = {
    val nullPointerException = assertThrows(
      classOf[NullPointerException],
      () => Utils.requireNotNull(null, "object can't be null"))
    assertEquals("object can't be null", nullPointerException.getMessage)
  }

  @Test def requireNotEmpty(): Unit = {
    assertFalse(Utils.requireNotEmpty(null))
    assertTrue(Utils.requireNotEmpty(new Array[Int](1)))
    assertTrue(Utils.requireNotEmpty("string"))
    assertTrue(Utils.requireNotEmpty(Traversable.canBuildFrom("Traversable")))
    assertTrue(Utils.requireNotEmpty(Iterable.canBuildFrom("Iterable")))

    val arrayList = new util.ArrayList[String](16)
    arrayList.add("arrayList")
    assertTrue(Utils.requireNotEmpty(arrayList))

    val hashMap = new util.HashMap[String, String](16)
    hashMap.put("hash", "map")
    assertTrue(Utils.requireNotEmpty(hashMap))

    assertTrue(Utils.requireNotEmpty())
  }

  @Test def requiredTest(): Unit = {
    assertThrows(classOf[IllegalArgumentException], () => Utils.required(false))
  }

  @Test def requireCheckJarFileTest(): Unit = {
    val jar: URL = new URL("http", "host", "file")
    val ioException = assertThrows(classOf[IOException], () => Utils.requireCheckJarFile(jar))
    assertEquals("JAR file path is invalid " + jar.toString, ioException.getMessage)
  }

  @Test def checkHttpURLTest(): Unit = {
    val url = "http://localhost"
    assertFalse(Utils.checkHttpURL(url))
  }
}
