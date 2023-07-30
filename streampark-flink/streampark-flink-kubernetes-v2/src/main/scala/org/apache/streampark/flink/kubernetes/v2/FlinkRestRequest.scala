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

import org.apache.streampark.flink.kubernetes.v2.FlinkRestRequest._
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkPipeOprState, JobSavepointDef, JobSavepointStatus}
import sttp.client3._
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.client3.ziojson._
import zio.json.{DeriveJsonCodec, JsonCodec, jsonField}
import zio.{IO, Task, ZIO}

import scala.util.chaining.scalaUtilChainingOps

type TriggerId = String

/**
 * Flink rest-api request.
 */
case class FlinkRestRequest(restUrl: String) {
  /**
   * Get all job overview info
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-overview
   */
  def listJobOverviewInfo: IO[Throwable, Vector[JobOverviewInfo]] = usingSttp { backend =>
    request
      .get(uri"$restUrl/jobs/overview")
      .response(asJson[JobOverviewRsp])
      .send(backend)
      .flattenBodyT
      .map(_.jobs)
  }

  /**
   * Get cluster overview
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#overview-1
   */
  def getClusterOverview: IO[Throwable, ClusterOverviewInfo] = usingSttp { backend =>
    request
      .get(uri"$restUrl/overview")
      .response(asJson[ClusterOverviewInfo])
      .send(backend)
      .flattenBodyT
  }

  /**
   * Get job manager configuration.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobmanager-config
   */
  def getJobmanagerConfig: IO[Throwable, Map[String, String]] = usingSttp { backend =>
    request
      .get(uri"$restUrl/jobmanager/config")
      .send(backend)
      .flattenBody
      .attemptBody(ujson.read(_).arr.map(item => item("key").str -> item("value").str).toMap)
  }

  /**
   * Cancels job.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-1
   */
  def cancelJob(jobId: String): IO[Throwable, Unit] =
    usingSttp { backend =>
      request
        .patch(uri"$restUrl/jobs/$jobId?mode=cancel")
        .send(backend)
        .unit
    }

  /**
   * Stops job with savepoint.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-stop
   */
  def stopJobWithSavepoint(jobId: String, sptReq: StopJobSptReq): IO[Throwable, TriggerId] =
    usingSttp { backend =>
      request
        .post(uri"$restUrl/jobs/$jobId/stop")
        .body(sptReq)
        .send(backend)
        .flattenBody
        .attemptBody(ujson.read(_)("request-id").str)
    }

  /**
   * Triggers a savepoint of job.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-savepoints
   */
  def triggerSavepoint(jobId: String, sptReq: TriggerSptReq): IO[Throwable, TriggerId] =
    usingSttp { backend =>
      request
        .post(uri"$restUrl/jobs/$jobId/savepoints")
        .body(sptReq)
        .send(backend)
        .flattenBody
        .attemptBody(ujson.read(_)("request-id").str)
    }

  /**
   * Get status of savepoint operation.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-savepoints-triggerid
   */
  def getSavepointOperationStatus(jobId: String, triggerId: String): IO[Throwable, JobSavepointStatus] =
    usingSttp { backend =>
      request
        .get(uri"$restUrl/jobs/$jobId/savepoints/$triggerId")
        .send(backend)
        .flattenBody
        .attemptBody { body =>
          val rspJson                  = ujson.read(body)
          val status                   = rspJson("status")("id").str.pipe(FlinkPipeOprState.ofRaw)
          val (location, failureCause) = rspJson("operation").objOpt match {
            case None => None -> None
            case Some(operation) =>
              val loc = operation.get("location").flatMap(_.strOpt)
              val failure = operation.get("failure-cause")
                .flatMap(_.objOpt.flatMap(map => map.get("stack-trace")))
                .flatMap(_.strOpt)
              loc -> failure
          }
          JobSavepointStatus(status, failureCause, location)
        }
    }
}

object FlinkRestRequest {

  val request = basicRequest

  // sttp client wrapper
  def usingSttp[A](request: SttpBackend[Task, Any] => IO[Throwable, A]): IO[Throwable, A] =
    ZIO.scoped {
      HttpClientZioBackend.scoped().flatMap(backend => request(backend))
    }

  implicit class RequestIOExceptionExt [A](requestIO: Task[Response[Either[ResponseException[String, String], A]]]) {
    @inline def flattenBodyT: IO[Throwable, A] = requestIO.flatMap(rsp => ZIO.fromEither(rsp.body))
  }

  implicit class RequestIOPlainExt(requestIO: Task[Response[Either[String, String]]]) {
    @inline def flattenBody: IO[Throwable, String] = requestIO.flatMap(rsp => ZIO.fromEither(rsp.body).mapError(new Exception(_)))
  }

  implicit class RequestIOTaskExt[A](requestIO: Task[String]) {
    @inline def attemptBody(f: String => A): IO[Throwable, A] = requestIO.flatMap { body => ZIO.attempt(f(body)) }
  }

  // --- Flink rest api models ---

  case class JobOverviewRsp(jobs: Vector[JobOverviewInfo])

  implicit val jobOverviewRspCodec: JsonCodec[JobOverviewRsp] = DeriveJsonCodec.gen[JobOverviewRsp]

  case class JobOverviewInfo(
      @jsonField("jid") jid: String,
      name: String,
      state: String,
      @jsonField("start-time") startTime: Long,
      @jsonField("end-time") endTime: Long,
      @jsonField("last-modification") lastModifyTime: Long,
      tasks: TaskStats)

  implicit val jobOverviewInfoCodec: JsonCodec[JobOverviewInfo] = DeriveJsonCodec.gen[JobOverviewInfo]

  case class TaskStats(
      total: Int,
      created: Int,
      scheduled: Int,
      deploying: Int,
      running: Int,
      finished: Int,
      canceling: Int,
      canceled: Int,
      failed: Int,
      reconciling: Int,
      initializing: Int)

  implicit val taskStatsCodec: JsonCodec[TaskStats] = DeriveJsonCodec.gen[TaskStats]

  case class ClusterOverviewInfo(
      @jsonField("flink-version") flinkVersion: String,
      @jsonField("taskmanagers") taskManagers: Int,
      @jsonField("slots-total") slotsTotal: Int,
      @jsonField("slots-available") slotsAvailable: Int,
      @jsonField("jobs-running") jobsRunning: Int,
      @jsonField("jobs-finished") jobsFinished: Int,
      @jsonField("jobs-cancelled") jobsCancelled: Int,
      @jsonField("jobs-failed") jobsFailed: Int)

  implicit val clusterOverviewInfoCodec: JsonCodec[ClusterOverviewInfo] = DeriveJsonCodec.gen[ClusterOverviewInfo]

  case class StopJobSptReq(
      drain: Boolean = false,
      formatType: Option[String] = None,
      targetDirectory: Option[String],
      triggerId: Option[String] = None)

  implicit val stopJobSptReqCodec: JsonCodec[StopJobSptReq] = DeriveJsonCodec.gen[StopJobSptReq]

  object StopJobSptReq {
    def apply(sptConf: JobSavepointDef): StopJobSptReq =
      StopJobSptReq(sptConf.drain, sptConf.formatType, sptConf.savepointPath, sptConf.triggerId)
  }

  case class TriggerSptReq(
      @jsonField("cancel-job") cancelJob: Boolean = false,
      formatType: Option[String] = None,
      @jsonField("target-directory") targetDirectory: Option[String],
      triggerId: Option[String] = None)

  implicit val triggerSptReqCodec: JsonCodec[TriggerSptReq] = DeriveJsonCodec.gen[TriggerSptReq]

  object TriggerSptReq {
    def apply(sptConf: JobSavepointDef): TriggerSptReq =
      TriggerSptReq(cancelJob = false, sptConf.formatType, sptConf.savepointPath, sptConf.triggerId)
  }

}
