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

import zio.{IO, ZIO}
import zio.ZIO.attempt
import zio.http.{Body, Client}
import zio.http.Method.{PATCH, POST}
import zio.json._

import java.nio.charset.Charset

import scala.language.implicitConversions
import scala.util.chaining.scalaUtilChainingOps

/** Flink rest-api request. */
case class FlinkRestRequest(restUrl: String) {

  type TriggerId = String

  /**
   * Get all job overview info
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-overview
   */
  def listJobOverviewInfo: IO[Throwable, Vector[JobOverviewInfo]] =
    for {
      res <- Client.request(s"$restUrl/jobs/overview")
      rs  <- res.body.asJson[JobOverviewResp]
    } yield rs.jobs

  /**
   * Get cluster overview
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#overview-1
   */
  def getClusterOverview: IO[Throwable, ClusterOverviewInfo] =
    for {
      res <- Client.request(s"$restUrl/overview")
      rs  <- res.body.asJson[ClusterOverviewInfo]
    } yield rs

  /**
   * Get job manager configuration.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobmanager-config
   */
  def getJobmanagerConfig: IO[Throwable, Map[String, String]] =
    for {
      res  <- Client.request(s"$restUrl/jobmanager/config")
      body <- res.body.asString
      rs   <- attempt {
                ujson
                  .read(body)
                  .arr
                  .map(item => item("key").str -> item("value").str)
                  .toMap
              }
    } yield rs

  /**
   * Cancels job.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-1
   */
  def cancelJob(jobId: String): IO[Throwable, Unit] = {
    Client.request(s"$restUrl/jobs/$jobId?mode=cancel", method = PATCH).unit
  }

  /**
   * Stops job with savepoint.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-stop
   */
  def stopJobWithSavepoint(jobId: String, sptReq: StopJobSptReq): IO[Throwable, TriggerId] =
    for {
      res  <- Client.request(s"$restUrl/jobs/$jobId/stop", method = POST, content = sptReq.toJson)
      body <- res.body.asString
      rs   <- attempt(ujson.read(body)("request-id").str)
    } yield rs

  /**
   * Triggers a savepoint of job.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-savepoints
   */
  def triggerSavepoint(jobId: String, sptReq: TriggerSptReq): IO[Throwable, TriggerId] =
    for {
      res  <- Client.request(s"$restUrl/jobs/$jobId/savepoints", method = POST, content = sptReq.toJson)
      body <- res.body.asString
      rs   <- attempt(ujson.read(body)("request-id").str)
    } yield rs

  /**
   * Get status of savepoint operation.
   * see: https://nightlies.apache.org/flink/flink-docs-master/docs/ops/rest_api/#jobs-jobid-savepoints-triggerid
   */
  def getSavepointOperationStatus(jobId: String, triggerId: String): IO[Throwable, JobSavepointStatus] =
    for {
      res  <- Client.request(s"$restUrl/jobs/$jobId/savepoints/$triggerId")
      body <- res.body.asString
      rs   <- attempt {
                val rspJson                  = ujson.read(body)
                val status                   = rspJson("status")("id").str.pipe(FlinkPipeOprState.ofRaw)
                val (location, failureCause) = rspJson("operation").objOpt match {
                  case None            => None -> None
                  case Some(operation) =>
                    val loc     = operation.get("location").flatMap(_.strOpt)
                    val failure = operation
                      .get("failure-cause")
                      .flatMap(_.objOpt.flatMap(map => map.get("stack-trace")))
                      .flatMap(_.strOpt)
                    loc -> failure
                }
                JobSavepointStatus(status, failureCause, location)
              }
    } yield rs
}

object FlinkRestRequest {

  implicit def autoProvideClientLayer[A](zio: ZIO[Client, Throwable, A]): IO[Throwable, A] =
    zio.provideLayer(Client.default)

  implicit def liftStringBody(content: String): Body = Body.fromString(content, charset = Charset.forName("UTF-8"))

  implicit class BodyExtension(body: Body) {
    def asJson[A](implicit decoder: JsonDecoder[A]): IO[Throwable, A] = for {
      data <- body.asString
      rsp  <- ZIO.fromEither(data.fromJson[A]).mapError(ParseJsonError)
    } yield rsp
  }

  case class ParseJsonError(msg: String) extends Exception(msg)

  // --- Flink rest api models ---

  case class JobOverviewResp(jobs: Vector[JobOverviewInfo])

  object JobOverviewResp {
    implicit val codec: JsonCodec[JobOverviewResp] = DeriveJsonCodec.gen[JobOverviewResp]
  }

  case class JobOverviewInfo(
      @jsonField("jid") jid: String,
      name: String,
      state: String,
      @jsonField("start-time") startTime: Long,
      @jsonField("end-time") endTime: Long,
      @jsonField("last-modification") lastModifyTime: Long,
      tasks: TaskStats)

  object JobOverviewInfo {
    implicit val codec: JsonCodec[JobOverviewInfo] = DeriveJsonCodec.gen[JobOverviewInfo]
  }

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

  object TaskStats {
    implicit val codec: JsonCodec[TaskStats] = DeriveJsonCodec.gen[TaskStats]
  }

  case class ClusterOverviewInfo(
      @jsonField("flink-version") flinkVersion: String,
      @jsonField("taskmanagers") taskManagers: Int,
      @jsonField("slots-total") slotsTotal: Int,
      @jsonField("slots-available") slotsAvailable: Int,
      @jsonField("jobs-running") jobsRunning: Int,
      @jsonField("jobs-finished") jobsFinished: Int,
      @jsonField("jobs-cancelled") jobsCancelled: Int,
      @jsonField("jobs-failed") jobsFailed: Int)

  object ClusterOverviewInfo {
    implicit val codec: JsonCodec[ClusterOverviewInfo] = DeriveJsonCodec.gen[ClusterOverviewInfo]
  }

  case class StopJobSptReq(
      drain: Boolean = false,
      formatType: Option[String] = None,
      targetDirectory: Option[String],
      triggerId: Option[String] = None)

  object StopJobSptReq {
    implicit val codec: JsonCodec[StopJobSptReq] = DeriveJsonCodec.gen[StopJobSptReq]

    def apply(sptConf: JobSavepointDef): StopJobSptReq =
      StopJobSptReq(sptConf.drain, sptConf.formatType, sptConf.savepointPath, sptConf.triggerId)
  }

  case class TriggerSptReq(
      @jsonField("cancel-job") cancelJob: Boolean = false,
      formatType: Option[String] = None,
      @jsonField("target-directory") targetDirectory: Option[String],
      triggerId: Option[String] = None)

  object TriggerSptReq {
    implicit val triggerSptReqCodec: JsonCodec[TriggerSptReq] = DeriveJsonCodec.gen[TriggerSptReq]

    def apply(sptConf: JobSavepointDef): TriggerSptReq =
      TriggerSptReq(cancelJob = false, sptConf.formatType, sptConf.savepointPath, sptConf.triggerId)
  }

}
