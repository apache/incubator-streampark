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
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.jupiter.api.Assertions.{assertDoesNotThrow, assertEquals, assertFalse, assertThrows, assertTrue}
import org.junit.jupiter.api.{AfterAll, AfterEach, BeforeEach, Test}

import java.io.{File, FileInputStream}
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
    if (!dir.exists()) dir.mkdirs() else FileUtils.forceDelete(dir)
  }

  @AfterEach
  def removeOutputDir(): Unit = {
    val dir = new File(outputDir)
    if (dir.exists()) FileUtils.deleteDirectory(dir)
  }


  @Test
  def testMkdirs(): Unit = {
    assertDoesNotThrow(LfsOperator.mkdirs(null))
    assertDoesNotThrow(LfsOperator.mkdirs(""))
    assertTrue(LfsOperator.exists(outputDir))

    // duplicate mkdirs
    assertDoesNotThrow {
      LfsOperator.mkdirs(s"$outputDir/test")
      LfsOperator.mkdirs(s"$outputDir/test")
    }
  }


  @Test
  def testExists(): Unit = {
    assertDoesNotThrow {
      val dir = s"$outputDir/tmp"
      val f = new File(dir)
      f.mkdirs
      assertTrue(LfsOperator.exists(f.getAbsolutePath))
      assertTrue(LfsOperator.exists(dir))
    }

    // path that does not exists
    assertFalse(LfsOperator.exists(null))
    assertFalse(LfsOperator.exists(""))
    assertFalse(LfsOperator.exists(s"$outputDir/233"))
  }


  @Test
  def testMkCleanDirs(): Unit = {
    assertDoesNotThrow {
      Array.fill(5)(genRandomFile(outputDir))
      assertEquals(new File(outputDir).list.length, 5)
      LfsOperator.mkCleanDirs(outputDir)
      val dir = new File(outputDir)
      assertTrue(dir.exists)
      assertTrue(dir.isDirectory)
      assertEquals(dir.list.length, 0)
    }

    // path that does not exists
    assertDoesNotThrow(LfsOperator.mkdirs(null))
    assertDoesNotThrow(LfsOperator.mkdirs(""))

    // clean dirs that does not exists
    assertTrue {
      LfsOperator.mkCleanDirs(s"$outputDir/114514")
      new File(s"$outputDir/114514").exists
    }
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
    assertDoesNotThrow(LfsOperator.delete(null))
    assertDoesNotThrow(LfsOperator.delete(""))
    assertDoesNotThrow(LfsOperator.delete(s"$outputDir/114514"))
  }


  @Test
  def testCopy(): Unit = {
    // copy to file / to directory
    assertDoesNotThrow {
      val file = genRandomFile(outputDir)

      def assertCopy(to: String, expectedOut: String): Unit = {
        LfsOperator.copy(file.getAbsolutePath, to)
        val output = new File(expectedOut)
        assertTrue(output.exists)
        assertTrue(file.length() == output.length())
      }

      assertCopy(s"$outputDir/out-1", s"$outputDir/out-1/${file.getName}")
      assertCopy(s"$outputDir/out-2/${file.getName}", s"$outputDir/out-2/${file.getName}")
      assertCopy(s"$outputDir/out-3/114514.dat", s"$outputDir/out-3/114514.dat")
    }

    // copy file that not exists
    assertDoesNotThrow {
      LfsOperator.copy(s"$outputDir/nobody.dat", s"$outputDir/out-5/nobody.dat")
      assertFalse(new File(s"$outputDir/out-5/nobody.dat").exists)
    }

    // copy directory
    val dir = genRandomDir(outputDir.concat("/in-1"))._1
    assertThrows(classOf[IllegalArgumentException], LfsOperator.copy(dir.getAbsolutePath, s"$outputDir/out-6"))

    // delete or not delete the original file
    assertDoesNotThrow {
      val file = genRandomFile(outputDir)

    }

    // non-overwritten or non-overwritten copy

  }


  @Test
  def testCopyDir(): Unit = {

  }


  @Test
  def testMove(): Unit = {
    // move file to directory
    assertDoesNotThrow {
      val sourceFile = genRandomFile(outputDir)
      val sourceMd5 = DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(sourceFile)))
      val targetPath = s"$outputDir/target-1"
      LfsOperator.move(sourceFile.getAbsolutePath, targetPath)

      val targetFile = new File(targetPath, sourceFile.getName)
      assertTrue(targetFile.exists)
      assertEquals(sourceMd5, DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(targetFile))))
    }

    // move directory to directory
    assertDoesNotThrow {
      val (sourceDir, sourceFiles) = genRandomDir(s"$outputDir/tmp")
      val targetPath = s"$outputDir/target-2"
      LfsOperator.move(sourceDir.getAbsolutePath, targetPath)
      val targetDir = new File(targetPath.concat("/tmp"))
      assertTrue(targetDir.exists)
      assertTrue(sourceFiles.map(_.getName).sorted.sameElements(targetDir.listFiles.map(_.getName).sorted))
    }

    // file that not exists
    assertFalse {
      LfsOperator.move(s"$outputDir/aha.dat", s"$outputDir/target-3")
      new File(s"$outputDir/target-3/aha.dat").exists
    }
    assertDoesNotThrow {
      val file = genRandomFile(outputDir)
      assertDoesNotThrow(LfsOperator.move(file.getAbsolutePath, null))
      assertDoesNotThrow(LfsOperator.move(file.getAbsolutePath, ""))
    }

    // duplicate move file
    assertDoesNotThrow {
      val file = genRandomFile(outputDir)
      val target = s"$outputDir/target-4"
      LfsOperator.move(file.getAbsolutePath, target)
      LfsOperator.move(file.getAbsolutePath, target)
    }

    // duplicate move directory
    assertDoesNotThrow {
      val dir = genRandomDir(s"$outputDir/tmp-5", 3)._1
      val target = s"$outputDir/target-5"
      LfsOperator.move(dir.getAbsolutePath, target)
      LfsOperator.move(dir.getAbsolutePath, target)
      assertTrue(new File(s"$target/tmp-5").exists)
      assertEquals(new File(s"$target/tmp-5").listFiles.length, 3)
    }
  }

}


