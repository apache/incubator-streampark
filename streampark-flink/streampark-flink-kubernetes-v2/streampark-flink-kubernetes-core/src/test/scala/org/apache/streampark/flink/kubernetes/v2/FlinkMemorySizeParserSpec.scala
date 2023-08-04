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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FlinkMemorySizeParserSpec extends AnyWordSpecLike with Matchers {

  "case-1: parse flink memory size text to bytes" in {

    val cases = Seq(
      "1b"         -> Some(1L),
      "1bytes"     -> Some(1L),
      "1k"         -> Some(1024L),
      "1kb"        -> Some(1024L),
      "1kibibytes" -> Some(1024L),
      "1m"         -> Some(1024 * 1024L),
      "1mb"        -> Some(1024 * 1024L),
      "1mebibytes" -> Some(1024 * 1024L),
      "1g"         -> Some(1024 * 1024 * 1024L),
      "1gb"        -> Some(1024 * 1024 * 1024L),
      "1gibibytes" -> Some(1024 * 1024 * 1024L),
      "1t"         -> Some(1024 * 1024 * 1024 * 1024L),
      "1tb"        -> Some(1024 * 1024 * 1024 * 1024L),
      "1tebibytes" -> Some(1024 * 1024 * 1024 * 1024L)
    )
    cases.foreach { case (in, expect) =>
      FlinkMemorySizeParser.parse(in).map(_.bytes) shouldBe expect
    }
  }

  "case-2: parse strange flink memory size text to bytes" in {

    val cases = Seq(
      "1 m"    -> Some(1024 * 1024L),
      " 1  m"  -> Some(1024 * 1024L),
      " 1 m  " -> Some(1024 * 1024L),
      "1 m  "  -> Some(1024 * 1024L),
      "  1m  " -> Some(1024 * 1024L),
      "  m  "  -> None,
      "m  "    -> None,
      "1024"   -> None,
      "1024 "  -> None,
      " 1024 " -> None
    )
    cases.foreach { case (in, expect) =>
      FlinkMemorySizeParser.parse(in).map(_.bytes) shouldBe expect
    }
  }

  "case-3: parse and convert memory unit" in {
    FlinkMemorySizeParser.parse("1g").map(_.mebiBytes) shouldBe Some(1024L)
    FlinkMemorySizeParser.parse("1g").map(_.kibiBytes) shouldBe Some(1024L * 1024L)
  }

}
