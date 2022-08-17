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

package com.streamxhub.streamx.flink.udf

import org.apache.flink.table.functions.ScalarFunction

import java.text.SimpleDateFormat
import java.util.Date

class HashCode(val factor: Int = 12) extends ScalarFunction {
  def eval(s: String): Int = s match {
    case null => 0
    case x => x.hashCode() * factor
  }
}

class DateFormat(val format: String = "yyyy-MM-dd HH:mm:ss") extends ScalarFunction {
  var sf: SimpleDateFormat = _

  def eval(time: Long): String = {
    if (sf == null) {
      sf = new SimpleDateFormat(format)
    }
    sf.format(new Date(time))
  }
}

class Length extends ScalarFunction {
  def eval(str: String): Int = {
    str match {
      case null => 0
      case x => x.length
    }
  }
}
