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

package org.apache.streampark.common.fs

import org.apache.streampark.common.fs.LfsOperatorTest.withTempDir

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Paths}

object LfsOperatorTest {

  def withTempDir(block: String => Unit): Unit = {
    val tempDirPath = Files.createTempDirectory("LfsOperatorTest-output")
    try {
      block(tempDirPath.toAbsolutePath.toString)
    } finally {
      FileUtils.deleteQuietly(tempDirPath.toFile)
    }
  }

}

class LfsOperatorTest {

  @Test
  def testMkdirs(): Unit = withTempDir {
    outputDir =>
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
  def testExists(): Unit = withTempDir {
    outputDir =>
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
  def testMkCleanDirs(): Unit = withTempDir {
    outputDir =>
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
  def listDir(): Unit = withTempDir {
    outputDir =>
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
  def testDelete(): Unit = withTempDir {
    outputDir =>
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

  // noinspection TypeAnnotation
  val md5Hex = (f: File) => DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(f)))
  // noinspection TypeAnnotation
  val sameFilesHex = (f1: Seq[File], f2: Seq[File]) =>
    f1.map(_.getName).sorted == f2.map(_.getName).sorted && f1.map(md5Hex).sorted == f2
      .map(md5Hex)
      .sorted

  @Test
  def testCopy(): Unit = withTempDir {
    outputDir =>
      // copy file to file path or directory path
      assertDoesNotThrow {
        val file = genRandomFile(outputDir)

        def assertCopy(to: String, expectedOut: String): Unit = {
          LfsOperator.copy(file.getAbsolutePath, to)
          val output = new File(expectedOut)
          assertTrue(output.exists)
          assertTrue(file.length() == output.length())
        }

        Files.createDirectory(Paths.get(outputDir, "out-1"))
        assertCopy(s"$outputDir/out-1", s"$outputDir/out-1/${file.getName}")
        Files.createDirectory(Paths.get(outputDir, "out-2"))
        assertCopy(s"$outputDir/out-2/${file.getName}", s"$outputDir/out-2/${file.getName}")
        Files.createDirectory(Paths.get(outputDir, "out-3"))
        assertCopy(s"$outputDir/out-3/114514.dat", s"$outputDir/out-3/114514.dat")
      }

      // copy file that not exists
      assertDoesNotThrow {
        LfsOperator.copy(s"$outputDir/nobody.dat", s"$outputDir/out-5/nobody.dat")
        assertFalse(new File(s"$outputDir/out-5/nobody.dat").exists)
      }

      // copy directory
      val dir = genRandomDir(outputDir.concat("/in-1"))._1
      assertThrows(
        classOf[IllegalArgumentException],
        LfsOperator.copy(dir.getAbsolutePath, s"$outputDir/out-6"))

      // delete or not delete the original file
      assertDoesNotThrow {
        // not delete origin file
        val file = genRandomFile(outputDir)
        LfsOperator.copy(file.getAbsolutePath, s"$outputDir/out-7", delSrc = false)
        assertTrue(file.exists)
        // delete origin file
        LfsOperator.copy(file.getAbsolutePath, s"$outputDir/out-8", delSrc = true)
        assertFalse(file.exists)
      }

      // overwritten or non-overwritten copy
      val file = genRandomFile(outputDir, "114514-233.dat")
      // overwritten
      assertDoesNotThrow {
        val out = genRandomFile(s"$outputDir/out-9", "114514-233.dat")
        val md5Before = md5Hex(out)
        LfsOperator.copy(file.getAbsolutePath, out.getAbsolutePath, overwrite = true)
        val md5After = md5Hex(new File(out.getAbsolutePath))
        assertNotEquals(md5Before, md5After)
        assertEquals(md5After, md5Hex(file))
      }
      // non-overwritten
      assertDoesNotThrow {
        val out = genRandomFile(s"$outputDir/out-10", "114514-233.dat")
        val md5Before = md5Hex(out)
        LfsOperator.copy(file.getAbsolutePath, out.getAbsolutePath, overwrite = false)
        val md5After = md5Hex(new File(out.getAbsolutePath))
        assertEquals(md5Before, md5After)
        assertNotEquals(md5After, md5Hex(file))
      }

  }

  @Test
  def testCopyDir(): Unit = withTempDir {
    outputDir =>
      // copy dir
      assertDoesNotThrow {
        val sourceDir = genRandomDir(s"$outputDir/in-1")._1
        val target = s"$outputDir/out-1"
        LfsOperator.copyDir(sourceDir.getAbsolutePath, target)
        val targetDir = new File(target)
        assertTrue(targetDir.exists)
        assertTrue(targetDir.isDirectory)
        assertTrue(sameFilesHex(sourceDir.listFiles, targetDir.listFiles))
      }

      // copy directory that not exists
      assertDoesNotThrow {
        LfsOperator.copyDir(s"$outputDir/in-2", s"$outputDir/out-2")
        assertFalse(new File(s"$outputDir/out-2").exists)
        LfsOperator.copyDir("", s"$outputDir/out-2")
        LfsOperator.copyDir(null, s"$outputDir/out-2")
      }

      // copy file
      assertThrows(
        classOf[IllegalArgumentException],
        LfsOperator.copyDir(genRandomFile(outputDir).getAbsolutePath, s"$outputDir/out-3"))

      // delete or not delete the original dir
      assertDoesNotThrow {
        // not delete origin dir
        val sourceDir = genRandomDir(s"$outputDir/in-4")._1
        LfsOperator.copyDir(sourceDir.getAbsolutePath, s"$outputDir/out-4", delSrc = false)
        assertTrue(sourceDir.exists)
        // delete origin dir
        LfsOperator.copyDir(sourceDir.getAbsolutePath, s"$outputDir/out-5", delSrc = true)
        assertFalse(sourceDir.exists)
      }

  }

  @Test
  def testMove(): Unit = withTempDir {
    outputDir =>
      // move file to directory
      assertDoesNotThrow {
        val sourceFile = genRandomFile(outputDir)
        val sourceMd5 = DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(sourceFile)))
        val targetPath = s"$outputDir/target-1"
        LfsOperator.move(sourceFile.getAbsolutePath, targetPath)

        val targetFile = new File(targetPath, sourceFile.getName)
        assertTrue(targetFile.exists)
        assertEquals(
          sourceMd5,
          DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(targetFile))))
      }

      // move directory to directory
      assertDoesNotThrow {
        val (sourceDir, sourceFiles) = genRandomDir(s"$outputDir/tmp")
        val targetPath = s"$outputDir/target-2"
        LfsOperator.move(sourceDir.getAbsolutePath, targetPath)
        val targetDir = new File(targetPath.concat("/tmp"))
        assertTrue(targetDir.exists)
        assertTrue(
          sourceFiles.map(_.getName).sorted.sameElements(targetDir.listFiles.map(_.getName).sorted))
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

  @Test
  def testfileMd5(): Unit = {
    assertThrows(classOf[IllegalArgumentException], LfsOperator.fileMd5(null))
    assertThrows(classOf[IllegalArgumentException], LfsOperator.fileMd5(""))
    assertThrows(classOf[IllegalArgumentException], LfsOperator.fileMd5("ttt/144514.dat"))
  }

}
