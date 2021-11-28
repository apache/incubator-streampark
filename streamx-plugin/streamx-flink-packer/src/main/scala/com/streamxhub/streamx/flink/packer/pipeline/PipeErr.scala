/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.flink.packer.pipeline

import javax.annotation.Nullable

/**
 * Error details of building pipeline.
 *
 * @param summary   summary of error.
 * @param exception exception stack.
 */
case class PipeErr(summary: String, @Nullable exception: Throwable) {

  val nonEmpty: Boolean = (summary != null && summary.nonEmpty) || exception != null
  val isEmpty: Boolean = !nonEmpty

  def exceptStackTraceContent: String = if (exception == null) "" else exception.getStackTrace.mkString("\n")
}


object PipeErr {
  def empty(): PipeErr = PipeErr("", null)
}
