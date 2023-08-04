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

import org.apache.streampark.flink.kubernetes.v2.model.JobDef.{DesiredState, UpgradeMode}
import org.apache.streampark.flink.kubernetes.v2.model.JobDef.DesiredState.DesiredState
import org.apache.streampark.flink.kubernetes.v2.model.JobDef.UpgradeMode.UpgradeMode

import org.apache.flink.v1beta1.{flinkdeploymentspec, flinksessionjobspec}

import scala.jdk.CollectionConverters.seqAsJavaListConverter
import scala.util.chaining.scalaUtilChainingOps

/**
 * Job definition.
 * Type safe mirror for [[org.apache.flink.v1beta1.flinkdeploymentspec.Job]] and [[org.apache.flink.v1beta1.flinksessionjobspec.Job]]
 *
 * @param jarURI                Optional URI of the job jar that only supports local file likes "/streampark/ws/assets/flink-faker-0.5.3.jar"
 * @param parallelism           Parallelism of the Flink job.
 * @param entryClass            Fully qualified main class name of the Flink job.
 * @param args                  Arguments for the Flink job main class.
 * @param state                 Desired state for the job.
 * @param upgradeMode           Upgrade mode of the Flink job.
 * @param savepointTriggerNonce Nonce used to manually trigger savepoint for the running job. In order to trigger a savepoint,
 *                              change the number to anything other than the current value.
 * @param initialSavepointPath  Savepoint path used by the job the first time it is deployed.
 *                              Upgrades/redeployments will not be affected.
 * @param allowNonRestoredState Allow checkpoint state that cannot be mapped to any job vertex in tasks.
 */
case class JobDef(
    jarURI: String,
    parallelism: Int,
    entryClass: Option[String] = None,
    args: Array[String] = Array.empty,
    state: DesiredState = DesiredState.RUNNING,
    upgradeMode: UpgradeMode = UpgradeMode.STATELESS,
    savepointTriggerNonce: Option[Long] = None,
    initialSavepointPath: Option[String] = None,
    allowNonRestoredState: Option[Boolean] = None) {

  // noinspection DuplicatedCode
  def toFlinkDeploymentJobSpec: flinkdeploymentspec.Job = new flinkdeploymentspec.Job().pipe { spec =>
    spec.setJarURI(jarURI)
    spec.setParallelism(parallelism)
    entryClass.foreach(spec.setEntryClass)
    if (args.nonEmpty) spec.setArgs(args.toList.asJava)
    spec.setState(DesiredState.toFlinkDeploymentEnum(state))
    spec.setUpgradeMode(UpgradeMode.toFlinkDeploymentEnum(upgradeMode))

    savepointTriggerNonce.foreach(spec.setSavepointTriggerNonce(_))
    initialSavepointPath.foreach(spec.setInitialSavepointPath)
    allowNonRestoredState.foreach(spec.setAllowNonRestoredState(_))
    spec
  }

  // noinspection DuplicatedCode
  def toFlinkSessionJobSpec: flinksessionjobspec.Job = new flinksessionjobspec.Job().pipe { spec =>
    spec.setJarURI(jarURI)
    spec.setParallelism(parallelism)
    entryClass.foreach(spec.setEntryClass)
    if (args.nonEmpty) spec.setArgs(args.toList.asJava)
    spec.setState(DesiredState.toFlinkSessionJobEnum(state))
    spec.setUpgradeMode(UpgradeMode.toFlinkSessionJobEnum(upgradeMode))

    savepointTriggerNonce.foreach(spec.setSavepointTriggerNonce(_))
    initialSavepointPath.foreach(spec.setInitialSavepointPath)
    allowNonRestoredState.foreach(spec.setAllowNonRestoredState(_))
    spec
  }

}

object JobDef {

  /**
   * see:
   * [[org.apache.flink.v1beta1.flinkdeploymentspec.Job.State]]
   * [[org.apache.flink.v1beta1.flinksessionjobspec.Job.State]]
   */
  object DesiredState extends Enumeration {
    type DesiredState = Value
    val RUNNING, SUSPENDED = Value

    def toFlinkDeploymentEnum(state: DesiredState): flinkdeploymentspec.Job.State = state match {
      case RUNNING   => flinkdeploymentspec.Job.State.RUNNING
      case SUSPENDED => flinkdeploymentspec.Job.State.SUSPENDED
    }

    def toFlinkSessionJobEnum(state: DesiredState): flinksessionjobspec.Job.State = state match {
      case RUNNING   => flinksessionjobspec.Job.State.RUNNING
      case SUSPENDED => flinksessionjobspec.Job.State.SUSPENDED
    }
  }

  /**
   * see:
   * [[org.apache.flink.v1beta1.flinkdeploymentspec.Job.UpgradeMode]]
   * [[org.apache.flink.v1beta1.flinksessionjobspec.Job.UpgradeMode]]
   */
  object UpgradeMode extends Enumeration {
    type UpgradeMode = Value
    val SAVEPOINT, LASTSTATE, STATELESS = Value

    def toFlinkDeploymentEnum(mode: UpgradeMode): flinkdeploymentspec.Job.UpgradeMode = mode match {
      case SAVEPOINT => flinkdeploymentspec.Job.UpgradeMode.SAVEPOINT
      case LASTSTATE => flinkdeploymentspec.Job.UpgradeMode.LASTSTATE
      case STATELESS => flinkdeploymentspec.Job.UpgradeMode.STATELESS
    }

    def toFlinkSessionJobEnum(mode: UpgradeMode): flinksessionjobspec.Job.UpgradeMode = mode match {
      case SAVEPOINT => flinksessionjobspec.Job.UpgradeMode.SAVEPOINT
      case LASTSTATE => flinksessionjobspec.Job.UpgradeMode.LASTSTATE
      case STATELESS => flinksessionjobspec.Job.UpgradeMode.STATELESS
    }
  }

}
