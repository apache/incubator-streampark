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

package org.apache.streampark.flink.kubernetes.v2.model

import org.apache.streampark.flink.kubernetes.v2.model.FlinkPipeOprState.FlinkPipeOprState

/**
 * Flink Job savepoint status.
 *
 * @param state        The state of the savepoint process.
 * @param failureCause The cause of failure.
 * @param location     The location of the savepoint.
 */
case class JobSavepointStatus(state: FlinkPipeOprState, failureCause: Option[String], location: Option[String]) {
  lazy val isCompleted = state == FlinkPipeOprState.Completed
  lazy val isFailed    = failureCause.isDefined
}

object FlinkPipeOprState extends Enumeration {
  type FlinkPipeOprState = Value

  val Completed  = Value("COMPLETED")
  val InProgress = Value("IN_PROGRESS")
  val Unknown    = Value("UNKNOWN")

  def ofRaw(rawValue: String): FlinkPipeOprState =
    FlinkPipeOprState.values.find(_.toString == rawValue).getOrElse(FlinkPipeOprState.Unknown)
}
