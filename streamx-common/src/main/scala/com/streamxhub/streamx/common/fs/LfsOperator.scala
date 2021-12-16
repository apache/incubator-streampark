/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.common.fs

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.common.util.Utils.{isAnyBank, notEmpty}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils

import java.io.{File, FileInputStream}

/**
 * Local File System (aka LFS) Operator
 */
//noinspection DuplicatedCode
object LfsOperator extends FsOperator with Logger {

  override def exists(path: String): Boolean = {
    StringUtils.isNotBlank(path) && new File(path).exists()
  }

  override def mkdirs(path: String): Unit = {
    if (!isAnyBank(path)) {
      FileUtils.forceMkdir(new File(path))
    }
  }

  override def delete(path: String): Unit = {
    if (notEmpty(path)) {
      val file = new File(path)
      if (file.exists()) {
        FileUtils.forceDelete(file)
      }
    }
  }

  override def move(srcPath: String, dstPath: String): Unit = {
    if (!isAnyBank(srcPath, dstPath)) {
      val srcFile = new File(srcPath)
      val dstFile = new File(dstPath)
      require(srcFile.exists(), "[StreamX] LFsOperator.move: Source must be exists")
      if (srcFile.getCanonicalPath != dstFile.getCanonicalPath) {
        if (dstFile.isDirectory) {
          FileUtils.moveToDirectory(srcFile, dstFile, true)
        } else {
          if (dstFile.exists()) {
            dstFile.delete()
          }
          FileUtils.moveFile(srcFile, dstFile)
        }
      }
    }
  }

  override def upload(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    if (new File(srcPath).isDirectory) {
      copyDir(srcPath, dstPath, delSrc, overwrite)
    } else {
      copy(srcPath, dstPath, delSrc, overwrite)
    }
  }

  override def copy(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    if (isAnyBank(srcPath, dstPath)) return
    val srcFile = new File(srcPath)
    if (!srcFile.exists) return
    var dstFile = new File(dstPath)
    if (dstFile.isDirectory) dstFile = new File(dstFile.getAbsolutePath.concat("/").concat(srcFile.getName))

    val shouldCopy = {
      if (overwrite || !dstFile.exists) true
      else srcFile.getCanonicalPath != dstFile.getCanonicalPath
    }
    if (shouldCopy) {
      FileUtils.copyFile(srcFile, dstFile)
      if (delSrc) FileUtils.forceDelete(srcFile)
    }
  }

  override def copyDir(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    if (isAnyBank(srcPath, dstPath)) return
    val srcFile = new File(srcPath)
    if (!srcFile.exists) return
    val dstFile = new File(dstPath)

    val shouldCopy = {
      if (overwrite || !dstFile.exists) true
      else srcFile.getCanonicalPath != dstFile.getCanonicalPath
    }
    if (shouldCopy) {
      FileUtils.copyDirectory(srcFile, dstFile)
      if (delSrc) FileUtils.deleteDirectory(srcFile)
    }
  }

  override def fileMd5(path: String): String = {
    require(path != null && path.nonEmpty, s"[StreamX] LFsOperator.fileMd5: file must not be null.")
    DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(path)))
  }

  /**
   * Force delete directory and recreate it.
   */
  def mkCleanDirs(path: String): Unit = {
    delete(path)
    mkdirs(path)
  }

  /**
   * list file under directory, one level of traversal only
   */
  def listDir(path: String): Array[File] = new File(path) match {
    case f if !f.exists => Array()
    case f if f.isFile => Array(f)
    case f => f.listFiles()
  }


}



