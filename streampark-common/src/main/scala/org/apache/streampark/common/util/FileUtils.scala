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

import java.io._
import java.net.URL
import java.util
import scala.collection.JavaConversions._

object FileUtils extends org.apache.commons.io.FileUtils {

  lazy val fileTypes: Map[String, String] = {
    val maps = new util.HashMap[String, String]()
    maps.put("FFD8FF", "jpg")
    maps.put("89504E47", "png")
    maps.put("47494638", "gif")
    maps.put("49492A00227105008037", "tif")
    maps.put("424D228C010000000000", "bmp")
    maps.put("424D8240090000000000", "bmp")
    maps.put("424D8E1B030000000000", "bmp")
    maps.put("41433130313500000000", "dwg")

    maps.put("68746D6C3E", "html")
    maps.put("48544D4C207B0D0A0942", "css")
    maps.put("696B2E71623D696B2E71", "js")
    maps.put("7B5C727466315C616E73", "rtf")

    maps.put("38425053000100000000", "psd")
    maps.put("44656C69766572792D646174653A", "eml")
    maps.put("D0CF11E0A1B11AE10000", "doc")


    maps.put("D0CF11E0A1B11AE10000", "vsd")
    maps.put("5374616E64617264204A", "mdb")
    maps.put("252150532D41646F6265", "ps")
    maps.put("255044462D312E", "pdf")
    maps.put("75736167", "txt")

    maps.put("2E524D46000000120001", "rmvb")
    maps.put("464C5601050000000900", "flv")
    maps.put("00000020667479706D70", "mp4")
    maps.put("49443303000000002176", "mp3")
    maps.put("000001B", "mpg")
    maps.put("3026B2758E66CF11A6D9", "wmv")
    maps.put("57415645", "wav")
    maps.put("41564920", "avi")

    maps.put("4D546864", "mid")
    maps.put("504B0304", "zip")
    maps.put("52617221", "rar")
    maps.put("235468697320636F6E66", "ini")
    maps.put("504B03040A000000", "jar")
    maps.put("4D5A9000030000000400", "exe")

    maps.put("3C25402070616765206C", "jsp")
    maps.put("4D616E69666573742D56", "mf")
    maps.put("3C3F786D6C", "xml")
    maps.put("494E5345525420494E54", "sql")
    maps.put("7061636B616765207765", "java")
    maps.put("406563686F206F66660D", "bat")
    maps.put("1F8B0800000000000000", "gz")
    maps.put("6C6F67346A2E726F6F74", "properties")
    maps.put("CAFEBABE0000002E0041", "class")
    maps.put("49545346030000006000", "chm")
    maps.put("04000000010000001300", "mxp")
    maps.put("504B0304140006000800", "docx")
    maps.put("D0CF11E0A1B11AE10000", "wps")
    maps.put("6431303A637265617465", "torrent")

    maps.put("6D6F6F76", "mov")
    maps.put("FF575043", "wpd")
    maps.put("CFAD12FEC5FD746F", "dbx")
    maps.put("2142444E", "pst")
    maps.put("AC9EBD8F", "qdf")
    maps.put("E3828596", "pwl")
    maps.put("2E7261FD", "ram")
    maps.put("2E524D46", "rm")
    maps.toMap
  }

  private[this] def bytesToHexString(src: Array[Byte]): String = {
    val stringBuilder: StringBuilder = new StringBuilder
    if (src == null || src.length <= 0) return null
    for (i <- 0 until src.length) {
      val v: Int = src(i) & 0xFF
      val hv: String = Integer.toHexString(v).toUpperCase
      if (hv.length < 2) {
        stringBuilder.append(0)
      }
      stringBuilder.append(hv)
    }
    stringBuilder.toString
  }

  def getFileType(file: File): String = {
    if (!file.exists || !file.isFile) {
      throw new RuntimeException("The file does not exist or the path is a directory")
    }
    Utils.tryWithResource(new FileInputStream(file)) { in =>
      val b = new Array[Byte](4)
      in.read(b, 0, b.length)
      val fileCode = bytesToHexString(b)
      fileTypes.find(_._1.startsWith(fileCode)) match {
        case Some(f) => f._2
        case _ => null
      }
    }
  }

  def createTempDir(): File = {
    val TEMP_DIR_ATTEMPTS = 10000
    val baseDir = new File(System.getProperty("java.io.tmpdir"))
    val baseName = System.currentTimeMillis + "-"
    for (counter <- 0 until TEMP_DIR_ATTEMPTS) {
      val tempDir = new File(baseDir, baseName + counter)
      if (tempDir.mkdir) {
        return tempDir
      }
    }
    throw new IllegalStateException(s"[StreamPark] Failed to create directory within $TEMP_DIR_ATTEMPTS  attempts (tried $baseName 0 to $baseName ${TEMP_DIR_ATTEMPTS - 1})")
  }

  def exists(path: String): Unit = {
    require(path != null && path.nonEmpty && new File(path).exists(), s"[StreamPark] FileUtils.exists: file $path is not exist!")
  }

  def getPathFromEnv(env: String): String = {
    val path = System.getenv(env)
    require(Utils.notEmpty(path), s"[StreamPark] FileUtils.getPathFromEnv: $env is not set on system env")
    val file = new File(path)
    require(file.exists(), s"[StreamPark] FileUtils.getPathFromEnv: $env is not exist!")
    file.getAbsolutePath
  }

  def resolvePath(parent: String, child: String): String = {
    val file = new File(parent, child)
    require(file.exists, s"[StreamPark] FileUtils.resolvePath: ${file.getAbsolutePath} is not exist!")
    file.getAbsolutePath
  }

  def getSuffix(filename: String): String = {
    require(filename != null)
    filename.drop(filename.lastIndexOf("."))
  }

  def listFileAsURL(dirPath: String): util.List[URL] = {
    new File(dirPath) match {
      case x if x.exists() && x.isDirectory =>
        val files = x.listFiles()
        if (files != null && files.nonEmpty) {
          files.map(f => f.toURI.toURL).toList
        } else {
          util.Collections.emptyList()
        }
      case _ => util.Collections.emptyList()
    }
  }

  def equals(file1: File, file2: File): Boolean = {
    (file1, file2) match {
      case (a, b) if a == null || b == null => false
      case (a, b) if !a.exists() || !b.exists() => false
      case (a, b) if a.getAbsolutePath == b.getAbsolutePath => true
      case (a, b) =>
        val first = new BufferedInputStream(new FileInputStream(a))
        val second = new BufferedInputStream(new FileInputStream(b))
        if (first.available() != second.available()) false; else {
          while (true) {
            val firRead = first.read()
            val secRead = second.read()
            if (firRead != secRead) {
              Utils.close(first, second)
              return false
            }
            if (firRead == -1) {
              Utils.close(first, second)
              return true;
            }
          }
          true
        }
    }
  }

}
