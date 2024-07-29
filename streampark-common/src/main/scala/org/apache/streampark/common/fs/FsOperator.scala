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

import org.apache.streampark.common.enums.StorageType

object FsOperator {

  lazy val lfs: FsOperator = FsOperator.of(StorageType.LFS)

  lazy val hdfs: FsOperator = FsOperator.of(StorageType.HDFS)

  def of(storageType: StorageType): FsOperator = {
    storageType match {
      case StorageType.HDFS => HdfsOperator
      case StorageType.LFS => LfsOperator
      case _ => throw new UnsupportedOperationException(s"Unsupported storageType:$storageType")
    }
  }

}

abstract class FsOperator {

  def exists(path: String): Boolean

  def mkdirs(path: String): Unit

  def mkdirsIfNotExists(path: String): Unit = {
    if (!exists(path)) {
      mkdirs(path)
    }
  }

  def delete(path: String): Unit

  def mkCleanDirs(path: String): Unit

  def upload(srcPath: String, dstPath: String): Unit =
    upload(srcPath, dstPath, delSrc = false, overwrite = true)

  def copy(srcPath: String, dstPath: String): Unit =
    copy(srcPath, dstPath, delSrc = false, overwrite = true)

  def copyDir(srcPath: String, dstPath: String): Unit =
    copyDir(srcPath, dstPath, delSrc = false, overwrite = true)

  def upload(
      srcPath: String,
      dstPath: String,
      delSrc: Boolean = false,
      overwrite: Boolean = true): Unit

  def copy(
      srcPath: String,
      dstPath: String,
      delSrc: Boolean = false,
      overwrite: Boolean = true): Unit

  def copyDir(
      srcPath: String,
      dstPath: String,
      delSrc: Boolean = false,
      overwrite: Boolean = true): Unit

  def move(srcPath: String, dstPath: String): Unit

  def fileMd5(path: String): String

}
