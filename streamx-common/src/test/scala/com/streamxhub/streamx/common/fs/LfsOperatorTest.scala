/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.common.fs


import com.streamxhub.streamx.common.fs.LfsOperatorTest.outputDir
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.{assertDoesNotThrow, assertEquals, assertFalse, assertTrue}
import org.junit.jupiter.api.{AfterAll, AfterEach, BeforeEach, Test}

import java.io.File
import scala.language.implicitConversions

/**
 * @author Al-assad
 */
object LfsOperatorTest {

  val outputDir = "LfsOperatorTest-output/"

  @AfterAll
  def cleanTmpResource(): Unit = {
    val dir = new File(outputDir)
    if (dir.exists()) FileUtils.deleteDirectory(dir)
  }

}

class LfsOperatorTest {

  @BeforeEach
  def createOutputDir(): Unit = {
    val dir = new File(outputDir)
    if (!dir.exists()) dir.mkdirs()
  }

  @AfterEach
  def removeOutputDir(): Unit = {
    val dir = new File(outputDir)
    if (dir.exists()) FileUtils.deleteDirectory(dir)
  }


  @Test
  def testMkdirs(): Unit = {
    assertDoesNotThrow(() => LfsOperator.mkdirs(null))
    assertDoesNotThrow(() => LfsOperator.mkdirs(""))
    assertTrue(LfsOperator.exists(outputDir))
  }


  @Test
  def testExists(): Unit = {
    val dir = s"$outputDir/tmp"
    val f = new File(dir)
    f.mkdirs
    assertTrue(LfsOperator.exists(f.getAbsolutePath))
    assertTrue(LfsOperator.exists(dir))

    // path that does not exists
    assertFalse(LfsOperator.exists(null))
    assertFalse(LfsOperator.exists(""))
    assertFalse(LfsOperator.exists(s"$outputDir/233"))
  }


  @Test
  def testMkCleanDirs(): Unit = {
    Array.fill(5)(genRandomFile(outputDir))
    assertEquals(new File(outputDir).list.length, 5)
    LfsOperator.mkCleanDirs(outputDir)
    val dir = new File(outputDir)
    assertTrue(dir.exists)
    assertTrue(dir.isDirectory)
    assertEquals(dir.list.length, 0)

    // path that does not exists
    assertDoesNotThrow(() => LfsOperator.mkdirs(null))
    assertDoesNotThrow(() => LfsOperator.mkdirs(""))
  }


  @Test
  def listDir(): Unit = {
    // list directory
    assertTrue {
      val expectFs = genRandomDir(outputDir)._2
      val actualFs = LfsOperator.listDir(outputDir)
      expectFs.map(_.getName).sorted.sameElements(actualFs.map(_.getName).sorted)
    }

    // list file
    assertTrue {
      val file = genRandomFile(outputDir)
      val listFile = LfsOperator.listDir(file.getAbsolutePath)
      listFile.length == 1 && listFile.head.getName == file.getName
    }

    // path that does not exists
    assertTrue(LfsOperator.listDir("").isEmpty)
    assertTrue(LfsOperator.listDir(null).isEmpty)
    assertTrue(LfsOperator.listDir(s"$outputDir/114514").isEmpty)
  }


  @Test
  def testDelete(): Unit = {
    // delete directory
    assertFalse {
      val dir = s"$outputDir/tmp"
      genRandomDir(dir)
      LfsOperator.delete(dir)
      new File(dir).exists
    }

    // delete file
    assertFalse {
      val file = genRandomFile(outputDir)
      LfsOperator.delete(file.getAbsolutePath)
      file.exists
    }

    // path that does not exists
    assertDoesNotThrow(() => LfsOperator.delete(null))
    assertDoesNotThrow(() => LfsOperator.delete(""))
  }


  def testCopy(): Unit = {
  }


  def testCopyDir(): Unit = {

  }


  def testMove(): Unit = {

  }


}


