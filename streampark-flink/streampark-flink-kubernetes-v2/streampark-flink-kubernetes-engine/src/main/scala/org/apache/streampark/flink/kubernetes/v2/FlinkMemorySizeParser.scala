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

package org.apache.streampark.flink.kubernetes.v2

import scala.util.Try

object FlinkMemorySizeParser {

  private val pattern = raw"(\d+)\s*([a-zA-Z]+)".r

  def parse(text: String): Option[MemorySize] = Try {
    val trimmed = text.trim
    if (trimmed.isEmpty) return None

    pattern.findFirstMatchIn(text) match {
      case None          => None
      case Some(matched) =>
        val size = matched.group(1).toLong
        val unit = matched.group(2)
        Unit.all.find(u => u.units.contains(unit)) match {
          case None          => None
          case Some(hitUnit) => Some(MemorySize(size * hitUnit.multiplier))
        }
    }
  }.getOrElse(None)

  case class MemorySize(bytes: Long) {

    def kibiBytes: Long = bytes >> 10
    def mebiBytes: Long = bytes >> 20
    def gibiBytes: Long = bytes >> 30
    def tebiBytes: Long = bytes >> 40
  }

  sealed abstract class UnitADT(val units: Array[String], val multiplier: Long)
  object Unit {
    val all = Array(Bytes, KiloBytes, MegaBytes, GigaBytes, TeraBytes)
    case object Bytes     extends UnitADT(Array("b", "bytes"), 1L)
    case object KiloBytes extends UnitADT(Array("k", "kb", "kibibytes"), 1024L)
    case object MegaBytes extends UnitADT(Array("m", "mb", "mebibytes"), 1024L * 1024L)
    case object GigaBytes extends UnitADT(Array("g", "gb", "gibibytes"), 1024L * 1024L * 1024L)
    case object TeraBytes extends UnitADT(Array("t", "tb", "tebibytes"), 1024L * 1024L * 1024L * 1024L)
  }

}
