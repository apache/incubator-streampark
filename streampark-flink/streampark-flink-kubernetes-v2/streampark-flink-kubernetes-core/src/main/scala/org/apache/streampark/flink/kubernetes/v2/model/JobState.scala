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

import org.apache.streampark.flink.kubernetes.v2.model.JobState.JobState

/**
 * Original Flink Job State.
 * This enum is essentially equivalent to the meaning in the Flink REST API.
 * see: [[org.apache.flink.kubernetes.operator.api.status.JobStatus]]
 */
object JobState extends Enumeration {

  type JobState = Value
  val INITIALIZING, CREATED, RUNNING, FAILING, FAILED, CANCELLING, CANCELED, FINISHED, RESTARTING, SUSPENDED,
      RECONCILING = Value
  val UNKNOWN = Value

  def valueOf(raw: String): JobState = values.find(_.toString == raw).getOrElse(UNKNOWN)
  val maybeDeploying                 = Set(INITIALIZING, CREATED, RESTARTING, RECONCILING)
}

/**
 * Evaluated Job State.
 * This state is the result of a combination of Flink CR and REST API evaluations,
 * It can be converted directly to StreamPark [[org.apache.streampark.console.core.enums.FlinkAppState]]
 */
object EvalJobState extends Enumeration {

  type EvalJobState = Value

  val INITIALIZING, CREATED, RUNNING, FAILING, FAILED, CANCELLING, CANCELED, FINISHED, RESTARTING, SUSPENDED,
      RECONCILING = Value

  // copy from [[org.apache.streampark.console.core.enums.FlinkAppState]]
  val LOST, TERMINATED, OTHER = Value

  def of(state: JobState): EvalJobState = values.find(e => e.toString == state.toString).getOrElse(OTHER)

}
