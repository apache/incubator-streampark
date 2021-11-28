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

import scala.language.postfixOps

/**
 * Status of building pipeline instance.
 * state machine:
 * ┌───────────────────────────────────┐
 * │                      ┌─► success  │
 * │  pending ─► running ─┤            │
 * │                      └─► failure  │
 * └───────────────────────────────────┘
 *
 * @author Al-assad
 */
//noinspection TypeAnnotation
object PipeStatus extends Enumeration {

  type PipeStatus = Value

  val pending = Value(1)
  val running = Value(2)
  val success = Value(3)
  val failure = Value(4)

  def isEnd(status: PipeStatus): Boolean = status == success || status == failure

  def copy(status: PipeStatus): PipeStatus = status match {
    case pending => pending
    case success => success
    case running => running
    case failure => failure
  }
}

/**
 * Status of per step of building pipeline.
 * state machine:
 * ┌──────────────────────────────────┐
 * │                     ┌─► success  │
 * │ pending ─► running ─┤            │
 * │              │      └─► failure  │
 * │              │             │     │
 * │              │             ▼     │
 * │              └────────► skipped  │
 * └──────────────────────────────────┘
 *
 * @author Al-assad
 */
//noinspection TypeAnnotation
object StepStatus extends Enumeration {

  type StepStatus = Value

  val waiting = Value(1)
  val running = Value(2)
  val success = Value(3)
  val failure = Value(4)
  val skipped = Value(5)

  def isEnd(status: StepStatus): Boolean = status == success || status == failure || status == skipped

  def copy(status: StepStatus): StepStatus = status match {
    case waiting => waiting
    case running => running
    case success => success
    case failure => failure
    case skipped => skipped
  }

}
