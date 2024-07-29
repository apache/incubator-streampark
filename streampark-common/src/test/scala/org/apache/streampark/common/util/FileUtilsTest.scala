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

import org.mockito.MockitoSugar._
import org.scalatest.funsuite.AnyFunSuite

import java.io._

class FileUtilsTest extends AnyFunSuite {

  test("isJarFileType should return true for a valid jar file") {
    val jarFileContent = Array[Byte](0x50, 0x4B, 0x03, 0x04)
    val jarInputStream = new ByteArrayInputStream(jarFileContent)
    assert(FileUtils.isJarFileType(jarInputStream))
  }

  test("isJarFileType should return false for a non-jar file") {
    val textFileContent = Array[Byte](0x54, 0x45, 0x53, 0x54)
    val textInputStream = new ByteArrayInputStream(textFileContent)
    assert(!FileUtils.isJarFileType(textInputStream))
  }

  test("exists should return true if the file exists") {
    val existingFile = new File("existing_file.txt")
    existingFile.createNewFile()
    assert(FileUtils.exists(existingFile))
    existingFile.delete()
  }

  test("exists should return false if the file does not exist") {
    val nonExistingFile = new File("non_existing_file.txt")
    assert(!FileUtils.exists(nonExistingFile))
  }

  test("createTempDir should create a new temporary directory") {
    val tempDir = FileUtils.createTempDir()
    assert(tempDir.exists())
    assert(tempDir.isDirectory)
  }

  test("getPathFromEnv should return the absolute path of the specified environment variable") {
    val envVar = "TEMP"
    val path = FileUtils.getPathFromEnv(envVar)
    assert(path != null && path.nonEmpty)
    val file = new File(path)
    assert(file.exists())
  }

  test(
    "getPathFromEnv should throw IllegalArgumentException if the specified environment variable is not set") {
    val nonExistingEnvVar = "NON_EXISTING_ENV_VAR"
    assertThrows[IllegalArgumentException] {
      FileUtils.getPathFromEnv(nonExistingEnvVar)
    }
  }

  test("resolvePath should throw IllegalArgumentException if the file does not exist") {
    val nonExistingParent = "/tmp"
    val child = "child.txt"

    // Mock file and its behavior
    val mockFile = mock[File]
    when(mockFile.exists).thenReturn(false)

    assertThrows[IllegalArgumentException] {
      FileUtils.resolvePath(nonExistingParent, child)
    }
  }

  test("getSuffix should throw IllegalArgumentException if the filename is null") {
    val filename: String = null
    assertThrows[IllegalArgumentException] {
      FileUtils.getSuffix(filename)
    }
  }

  test("readInputStream should read data from the input stream into the byte array") {
    val inputData = "Hello"
    val inputStream = new ByteArrayInputStream(inputData.getBytes("UTF-8"))
    val byteArray = new Array[Byte](inputData.length)
    FileUtils.readInputStream(inputStream, byteArray)
    val result = new String(byteArray, "UTF-8")
    assert(result == inputData)
  }

  test("readFile should read the content of the file into a string") {
    val content = "Hello"
    val file = new File("test_file.txt")
    val outputStream = new FileOutputStream(file)
    outputStream.write(content.getBytes("UTF-8"))
    outputStream.close()
    val result = FileUtils.readFile(file)
    assert(result == content)
    file.delete()
  }

  test("readEndOfFile should read the last part of a file") {
    val content = "1234567890"
    val file = new File("test_file.txt")
    val outputStream = new FileOutputStream(file)
    outputStream.write(content.getBytes("UTF-8"))
    outputStream.close()
    val result = FileUtils.readEndOfFile(file, 5)
    assert(result.sameElements("67890".getBytes("UTF-8")))
    file.delete()
  }

  test(
    "readEndOfFile should read the entire file if the file size is less than the specified limit") {
    val content = "1234567890"
    val file = new File("test_file.txt")
    val outputStream = new FileOutputStream(file)
    outputStream.write(content.getBytes("UTF-8"))
    outputStream.close()
    val result = FileUtils.readEndOfFile(file, 15)
    assert(result.sameElements(content.getBytes("UTF-8")))
    file.delete()
  }

  test("readFileFromOffset should read the content of the file from a specified offset") {
    val content = "1234567890"
    val file = new File("test_file.txt")
    val outputStream = new FileOutputStream(file)
    outputStream.write(content.getBytes("UTF-8"))
    outputStream.close()
    val result = FileUtils.readFileFromOffset(file, 5, 5)
    assert(result.sameElements("67890".getBytes("UTF-8")))
    file.delete()
  }

  test(
    "readFileFromOffset should throw IllegalArgumentException if the startOffset is greater than the file length") {
    val file = new File("test_file.txt")
    assertThrows[IllegalArgumentException] {
      FileUtils.readFileFromOffset(file, 15, 5)
    }
  }

  test("tailOf should return null if the file does not exist") {
    val result = FileUtils.tailOf("/nonexistent_file.txt", 0, 5)
    assert(result == null)
  }
}
