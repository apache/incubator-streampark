/**
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

package com.streamxhub.spark.monitor.controller

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import org.json4s._

class BaseController {

  @transient
  implicit val format: AnyRef with Formats = Serialization.formats(NoTypeHints)

  @transient
  implicit val seconds: FiniteDuration = 60.second

  implicit def render(future:Future[_]):Render = {
    new Render(future)
  }

  class Render(future: Future[_]) {
    def render(): String = {
      write(Await.result(future, seconds))
    }
  }
}


