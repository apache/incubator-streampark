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
package org.apache.streampark.common

import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.function.Executable

import java.io.File

import scala.language.implicitConversions
import scala.util.Random

package object fs {

  implicit def funcToExecutable[R](func: => R): Executable = new Executable() {
    override def execute(): Unit = func
  }

  /**
   * generate a random binary file
   *
   * @param dir
   *   parent directory
   * @param size
   *   file size
   */
  def genRandomFile(
      dir: String,
      name: String = s"${java.util.UUID.randomUUID().toString}.dat",
      size: Int = 256): File = {
    val random = new Random()
    val c = new Array[Byte](size)
    random.nextBytes(c)
    val f = new File(dir, name)
    FileUtils.writeByteArrayToFile(f, c)
    f
  }

  /**
   * generate a random directory that contains some random files
   *
   * @return
   *   Directory File object -> Children random files array.
   */
  def genRandomDir(dirPath: String, childFileCount: Int = 5): (File, Array[File]) = {
    val dir = new File(dirPath)
    if (!dir.exists()) dir.mkdirs()
    dir -> Array.fill(childFileCount)(genRandomFile(dirPath))
  }

}
