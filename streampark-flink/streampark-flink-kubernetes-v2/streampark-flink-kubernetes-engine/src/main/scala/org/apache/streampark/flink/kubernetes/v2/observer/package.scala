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

import org.apache.streampark.common.conf.InternalConfigHolder
import org.apache.streampark.flink.kubernetes.v2.FlinkK8sConfig._
import org.apache.streampark.flink.kubernetes.v2.observer.AccessFlinkRestType.AccessFlinkRestType

import zio.{durationLong, Duration}

import scala.util.chaining.scalaUtilChainingOps

/**
 * Notes:
 * Entry point [[FlinkK8sObserver]].
 */
package object observer {

  type Namespace = String
  type Name      = String
  type AppId     = Long

  lazy val evalJobSnapParallelism: Int   = InternalConfigHolder.get(EVAL_FLINK_JOB_SNAPSHOT_PARALLELISM)
  lazy val evalJobSnapInterval: Duration = InternalConfigHolder.get[Long](EVAL_FLINK_JOB_SNAP_INTERVAL_MILLIS).millis

  lazy val restPollingInterval: Duration = InternalConfigHolder.get[Long](POLL_FLINK_REST_INTERVAL).millis
  lazy val restRetryInterval: Duration   = InternalConfigHolder.get[Long](RETRY_FLINK_REST_INTERVAL).millis

  val reachFlinkRestType: AccessFlinkRestType = InternalConfigHolder
    .get[String](REACH_FLINK_REST_TYPE)
    .pipe { plain =>
      AccessFlinkRestType.values
        .find(_.toString.equalsIgnoreCase(plain))
        .getOrElse(AccessFlinkRestType.IP)
    }

  object AccessFlinkRestType extends Enumeration {
    type AccessFlinkRestType = Value
    val DNS, IP = Value
  }

}
