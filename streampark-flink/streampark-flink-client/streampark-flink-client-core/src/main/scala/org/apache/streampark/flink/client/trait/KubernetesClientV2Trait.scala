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

package org.apache.streampark.flink.client.`trait`

import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.flink.client.`trait`.KubernetesClientV2.{StopJobFail, TriggerJobSavepointFail}
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.kubernetes.v2.model.{JobDef, JobSavepointDef}
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator
import org.apache.streampark.flink.kubernetes.v2.yamlMapper

import io.fabric8.kubernetes.api.model.Pod
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration.{Configuration, CoreOptions, PipelineOptions}
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions
import zio.ZIO

import scala.collection.mutable
import scala.jdk.CollectionConverters.asScalaBufferConverter
import scala.util.{Failure, Success, Try}

/** Flink K8s session/application mode cancel and Savepoint */
trait KubernetesClientV2Trait extends FlinkClientTrait {

  protected type FailureMessage = String

  protected val KUBERNETES_JM_CPU_KEY = "kubernetes.jobmanager.cpu"
  protected val KUBERNETES_JM_CPU_AMOUNT_KEY = "kubernetes.jobmanager.cpu.amount"
  protected val KUBERNETES_JM_CPU_DEFAULT = 1.0
  protected val KUBERNETES_JM_MEMORY_DEFAULT = "1600m"

  protected val KUBERNETES_TM_CPU_KEY = "kubernetes.taskmanager.cpu"
  protected val KUBERNETES_TM_CPU_AMOUNT_KEY = "kubernetes.taskmanager.cpu.amount"
  protected val KUBERNETES_TM_CPU_DEFAULT = -1.0
  protected val KUBERNETES_TM_MEMORY_DEFAULT = "1728m"

  protected val KUBERNETES_REST_SERVICE_EXPORTED_TYPE_KEY = "kubernetes.rest-service.exposed.type"

  override def setConfig(submitRequest: SubmitRequest, flinkConf: Configuration): Unit = {}

  implicit protected class FlinkConfMapOps(map: mutable.Map[String, String]) {
    def removeKey(key: String): mutable.Map[String, String] = { map -= key; map }
  }

  protected def unmarshalPodTemplate(yaml: String): Try[Pod] = {
    Try(yamlMapper.readValue(yaml, classOf[Pod]))
  }

  /** Generate JobDef */
  protected def genJobDef(
      flinkConfObj: Configuration,
      jarUriHint: Option[String]): Either[FailureMessage, JobDef] = {

    val jarUri = jarUriHint
      .orElse(flinkConfObj.getOption(PipelineOptions.JARS).flatMap(_.asScala.headOption))
      .getOrElse(return Left("Flink job uri should not be empty"))

    val parallel = flinkConfObj
      .getOption(CoreOptions.DEFAULT_PARALLELISM)
      .getOrElse(CoreOptions.DEFAULT_PARALLELISM.defaultValue())

    val args = flinkConfObj
      .getOption(ApplicationConfiguration.APPLICATION_ARGS)
      .map(_.asScala.toArray)
      .getOrElse(Array.empty[String])

    val entryClass = flinkConfObj
      .getOption(ApplicationConfiguration.APPLICATION_MAIN_CLASS)

    val savePointPath = flinkConfObj
      .getOption(SavepointConfigOptions.SAVEPOINT_PATH)

    val allowNonRestoredState = flinkConfObj
      .getOption(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE)
      .map(_.booleanValue())

    Right(
      JobDef(
        jarURI = jarUri,
        parallelism = parallel,
        entryClass = entryClass,
        args = args,
        initialSavepointPath = savePointPath,
        allowNonRestoredState = allowNonRestoredState
      ))
  }

  @throws[Exception]
  override def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse = {
    val effect =
      if (!cancelRequest.withSavepoint) {
        // cancel job
        FlinkK8sOperator
          .cancelJob(cancelRequest.id)
          .as(CancelResponse(null))
      } else {
        // stop job with savepoint
        val savepointDef = JobSavepointDef(
          drain = Option(cancelRequest.withDrain).getOrElse(false),
          savepointPath = Option(cancelRequest.savepointPath),
          formatType = Option(cancelRequest.nativeFormat)
            .map(if (_) JobSavepointDef.NATIVE_FORMAT else JobSavepointDef.CANONICAL_FORMAT)
        )
        FlinkK8sOperator
          .stopJob(cancelRequest.id, savepointDef)
          .flatMap {
            result =>
              if (result.isFailed) ZIO.fail(StopJobFail(result.failureCause.get))
              else ZIO.succeed(CancelResponse(result.location.orNull))
          }
      }

    def richMsg: String => String = s"[flink-cancel][appId=${cancelRequest.id}] " + _

    effect.runIOAsTry match {
      case Success(rsp) =>
        logInfo(richMsg("Cancel flink job successfully."))
        rsp
      case Failure(err) =>
        logError(
          richMsg(s"Cancel flink job fail in ${cancelRequest.executionMode.getName}_V2 mode!"),
          err)
        throw err
    }
  }

  @throws[Exception]
  override def doTriggerSavepoint(
      savepointRequest: TriggerSavepointRequest,
      flinkConf: Configuration): SavepointResponse = {

    val savepointDef = JobSavepointDef(
      savepointPath = Option(savepointRequest.savepointPath),
      formatType = Option(savepointRequest.nativeFormat)
        .map(if (_) JobSavepointDef.NATIVE_FORMAT else JobSavepointDef.CANONICAL_FORMAT)
    )

    def richMsg: String => String =
      s"[flink-trigger-savepoint][appId=${savepointRequest.id}] " + _

    FlinkK8sOperator
      .triggerJobSavepoint(savepointRequest.id, savepointDef)
      .flatMap {
        result =>
          if (result.isFailed) ZIO.fail(TriggerJobSavepointFail(result.failureCause.get))
          else ZIO.succeed(SavepointResponse(result.location.orNull))
      }
      .runIOAsTry match {
      case Success(rsp) =>
        logInfo(richMsg("Trigger flink job savepoint successfully."))
        rsp
      case Failure(err) =>
        logError(
          richMsg(
            s"Trigger flink job savepoint failed in ${savepointRequest.executionMode.getName}_V2 mode!"),
          err)
        throw err
    }
  }

}

object KubernetesClientV2 {

  case class StopJobFail(msg: String) extends Exception(msg)
  case class TriggerJobSavepointFail(msg: String) extends Exception(msg)
}
