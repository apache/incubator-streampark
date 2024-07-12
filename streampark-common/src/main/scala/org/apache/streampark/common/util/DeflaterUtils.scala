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

import org.apache.commons.lang3.StringUtils

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.{DataFormatException, Deflater, Inflater}

object DeflaterUtils {

  /** Compress the specified text */
  def zipString(text: String): String = {
    if (StringUtils.isBlank(text)) return ""
    // compression level (0 ~ 9): low to high
    // create a new deflater with the specified compression level
    val deflater = new Deflater(Deflater.BEST_COMPRESSION)
    // set the compressed input data
    deflater.setInput(text.getBytes)
    deflater.finish()
    val bytes = new Array[Byte](256)
    val outputStream = new ByteArrayOutputStream(256)
    while (!deflater.finished) {
      val length = deflater.deflate(bytes)
      outputStream.write(bytes, 0, length)
    }
    deflater.`end`()
    Base64.getEncoder.encodeToString(outputStream.toByteArray)
  }

  def unzipString(zipString: String): String = {
    val decode = Base64.getDecoder.decode(zipString)
    val inflater = new Inflater
    inflater.setInput(decode)
    val bytes = new Array[Byte](256)
    val outputStream = new ByteArrayOutputStream(256)
    try {
      while (!inflater.finished) { // decompress bytes array to the buffer
        val length = inflater.inflate(bytes)
        outputStream.write(bytes, 0, length)
      }
    } catch {
      case e: DataFormatException =>
        e.printStackTrace()
        return null
    } finally {
      inflater.`end`()
    }
    outputStream.toString
  }

}
