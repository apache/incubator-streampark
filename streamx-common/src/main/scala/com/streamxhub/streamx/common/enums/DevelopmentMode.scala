/*
 * Copyright (c) 2019 The StreamX Project
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
package com.streamxhub.streamx.common.enums

import scala.collection.immutable
import enumeratum.EnumEntry


sealed abstract class DevelopmentMode(val mode: String, val value: Int) extends EnumEntry {

}

object DevelopmentMode extends enumeratum.Enum[DevelopmentMode] {

  val values: immutable.IndexedSeq[DevelopmentMode] = findValues

  case object CUSTOMCODE extends DevelopmentMode("Custom Code", 1)

  case object PURESQL extends DevelopmentMode("Pure SQL", 2)

  def of(value: Int): DevelopmentMode = findValues.filter(_.value == value).head

}
