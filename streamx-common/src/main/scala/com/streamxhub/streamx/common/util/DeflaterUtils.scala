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
package com.streamxhub.streamx.common.util

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.{DataFormatException, Deflater, Inflater}

object DeflaterUtils {

  /**
   * 压缩
   */
  def zipString(text: String): String = {
    /**
     * 0 ~ 9 压缩等级 低到高<br>
     * public static final int BEST_COMPRESSION = 9;            最佳压缩的压缩级别<br>
     * public static final int BEST_SPEED = 1;                  压缩级别最快的压缩<br>
     * public static final int DEFAULT_COMPRESSION = -1;        默认压缩级别<br>
     * public static final int DEFAULT_STRATEGY = 0;            默认压缩策略<br>
     * public static final int DEFLATED = 8;                    压缩算法的压缩方法(目前唯一支持的压缩方法)<br>
     * public static final int FILTERED = 1;                    压缩策略最适用于大部分数值较小且数据分布随机分布的数据<br>
     * public static final int FULL_FLUSH = 3;                  压缩刷新模式，用于清除所有待处理的输出并重置拆卸器<br>
     * public static final int HUFFMAN_ONLY = 2;                仅用于霍夫曼编码的压缩策略<br>
     * public static final int NO_COMPRESSION = 0;              不压缩的压缩级别<br>
     * public static final int NO_FLUSH = 0;                    用于实现最佳压缩结果的压缩刷新模式<br>
     * public static final int SYNC_FLUSH = 2;                  用于清除所有未决输出的压缩刷新模式; 可能会降低某些压缩算法的压缩率<br>
     */
    //使用指定的压缩级别创建一个新的压缩器。
    val deflater = new Deflater(Deflater.BEST_COMPRESSION)
    //设置压缩输入数据。
    deflater.setInput(text.getBytes)
    //当被调用时，表示压缩应该以输入缓冲区的当前内容结束。
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
    //设置解压缩的输入数据。
    inflater.setInput(decode)
    val bytes = new Array[Byte](256)
    val outputStream = new ByteArrayOutputStream(256)
    try {
      while (!inflater.finished) { //将字节解压缩到指定的缓冲区中。
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
