/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes.watcher

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode.{APPLICATION, SESSION}
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobStatusChangeEvent
import com.streamxhub.streamx.flink.kubernetes.model._
import com.streamxhub.streamx.flink.kubernetes.{ChangeEventBus, FlinkTrackController, JobStatusWatcherConfig, KubernetesRetriever}
import io.fabric8.kubernetes.client.Watcher.Action
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.util.Timeout
import org.json4s.{DefaultFormats, JNothing, JNull}
import org.json4s.JsonAST.JArray
import org.json4s.jackson.JsonMethods.parse

import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.Nonnull
import javax.annotation.concurrent.ThreadSafe
import scala.collection.JavaConversions._
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}

/**
 * Watcher for continuously monitor flink job status on kubernetes-mode,
 * the traced flink identifiers from FlinkTrackCachePool.trackIds, the traced
 * result of flink jobs status would written to FlinkTrackCachePool.jobStatuses.
 *
 * author:Al-assad
 */
@ThreadSafe
class FlinkJobStatusWatcher(conf: JobStatusWatcherConfig = JobStatusWatcherConfig.defaultConf)
                           (implicit val trackController: FlinkTrackController,
                            implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {

  private val trackTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trackTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trackTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  /**
   * stop watcher process
   */
  override def doStart(): Unit = {
    timerSchedule = timerExec.scheduleAtFixedRate(() => doWatch(), 0, conf.requestIntervalSec, TimeUnit.SECONDS)
    logInfo("[flink-k8s] FlinkJobStatusWatcher started.")
  }

  /**
   * stop watcher process
   */
  override def doStop(): Unit = {
    // interrupt all running threads
    timerSchedule.cancel(true)
    logInfo("[flink-k8s] FlinkJobStatusWatcher stopped.")
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  override def doClose(): Unit = {
    timerExec.shutdownNow()
    trackTaskExecutor.shutdownNow()
    logInfo("[flink-k8s] FlinkJobStatusWatcher closed.")
  }

  /**
   * single flink job status tracking task
   */
  override def doWatch(): Unit = {
    // get all legal tracking ids
    val trackIds = Try(trackController.collectAllTrackIds()).filter(_.nonEmpty).getOrElse(return)

    // retrieve flink job status in thread pool
    val tracksFuture: Set[Future[Option[JobStatusCV]]] = trackIds.map { id =>

      val future = Future {
        id.executeMode match {
          case SESSION => touchSessionJob(id)
          case APPLICATION => touchApplicationJob(id)
        }
      }

      future onComplete (_.getOrElse(None) match {
        case Some(jobState) =>
          val trackId = id.copy(jobId = jobState.jobId)
          val latest: JobStatusCV = trackController.jobStatuses.get(trackId)
          if (latest == null || latest.jobState != jobState.jobState || latest.jobId != jobState.jobId) {
            // put job status to cache
            trackController.jobStatuses.put(trackId, jobState)
            // set jobId to trackIds
            trackController.trackIds.update(trackId)
            eventBus.postSync(FlinkJobStatusChangeEvent(trackId, jobState))
          }
          if (FlinkJobState.isEndState(jobState.jobState)) {
            // remove trackId from cache of job that needs to be untracked
            trackController.unTracking(trackId)
            if (trackId.executeMode == APPLICATION) {
              trackController.endpoints.invalidate(trackId.toClusterKey)
            }
          }
        case _ =>
      })

      future
    }

    // blocking until all future are completed or timeout is reached
    Try(Await.ready(Future.sequence(tracksFuture), conf.requestTimeoutSec seconds))
      .failed.map { _ =>
      logInfo(s"[FlinkJobStatusWatcher] tracking flink job status on kubernetes mode timeout," +
        s" limitSeconds=${conf.requestTimeoutSec}," +
        s" trackIds=${trackIds.mkString(",")}")
    }
  }

  /**
   * Get flink status information from kubernetes-native-session cluster.
   * When the flink-cluster-client request fails, the job state would be
   * LOST or SILENT.
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchSessionJob(@Nonnull trackId: TrackId): Option[JobStatusCV] = {
    val pollEmitTime = System.currentTimeMillis
    val clusterId = trackId.clusterId
    val namespace = trackId.namespace
    val appId = trackId.appId
    val jobId = trackId.jobId

    val rsMap = touchSessionAllJob(clusterId, namespace, appId).toMap
    val id = TrackId.onSession(namespace, clusterId, appId, jobId)
    val jobState = rsMap.get(id).filter(_.jobState != FlinkJobState.SILENT).getOrElse {
      val preCache = trackController.jobStatuses.get(id)
      val state = inferSilentOrLostFromPreCache(preCache)
      val nonFirstSilent = state == FlinkJobState.SILENT && preCache != null && preCache.jobState == FlinkJobState.SILENT
      if (nonFirstSilent) {
        JobStatusCV(jobState = state, jobId = id.jobId, pollEmitTime = preCache.pollEmitTime, pollAckTime = preCache.pollAckTime)
      } else {
        JobStatusCV(jobState = state, jobId = id.jobId, pollEmitTime = pollEmitTime, pollAckTime = System.currentTimeMillis)
      }
    }

    Some(jobState)
  }

  /**
   * Get all flink job status information from kubernetes-native-session cluster.
   * The empty array will returned when the k8s-client or flink-cluster-client
   * request fails.
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  protected[kubernetes] def touchSessionAllJob(@Nonnull clusterId: String, @Nonnull namespace: String, @Nonnull appId: Long): Array[(TrackId, JobStatusCV)] = {
    lazy val defaultResult = Array.empty[(TrackId, JobStatusCV)]
    val pollEmitTime = System.currentTimeMillis
    val jobDetails = listJobsDetails(ClusterKey(SESSION, namespace, clusterId)).getOrElse(return defaultResult).jobs
    if (jobDetails.isEmpty) {
      defaultResult
    } else {
      jobDetails.map { d =>
        TrackId.onSession(namespace, clusterId, appId, d.jid) -> d.toJobStatusCV(pollEmitTime, System.currentTimeMillis)
      }
    }
  }

  /**
   * Get flink status information from kubernetes-native-application cluster.
   * When the flink-cluster-client request fails, will infer the job statue
   * from k8s events.
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchApplicationJob(@Nonnull trackId: TrackId): Option[JobStatusCV] = {
    implicit val pollEmitTime: Long = System.currentTimeMillis
    val clusterId = trackId.clusterId
    val namespace = trackId.namespace

    val jobDetails = listJobsDetails(ClusterKey(APPLICATION, namespace, clusterId))
    lazy val k8sInferResult = inferApplicationFlinkJobStateFromK8sEvent(trackId)
    jobDetails match {
      case Some(details) =>
        if (details.jobs.isEmpty) k8sInferResult; else {
          // just receive the first result
          val jobDetail = details.jobs.head.toJobStatusCV(pollEmitTime, System.currentTimeMillis)
          Some(jobDetail)
        }
      case _ => k8sInferResult
    }
  }

  /**
   * list flink jobs details
   */
  private def listJobsDetails(clusterKey: ClusterKey): Option[JobDetails] = {
    // get flink rest api
    var clusterRestUrl = trackController.getClusterRestUrl(clusterKey).filter(_.nonEmpty).getOrElse(return None)
    // list flink jobs from rest api
    Try(callJobsOverviewsApi(clusterRestUrl)).recover { case _ =>
      clusterRestUrl = trackController.refreshClusterRestUrl(clusterKey).getOrElse(return None)
      Try(callJobsOverviewsApi(clusterRestUrl)).recover { case ex =>
        logInfo(s"failed to list remote flink jobs on kubernetes-native-mode cluster, errorStack=${ex.getMessage}")
        None
      }.get
    }.get
  }

  /**
   * list flink jobs details from rest api
   */
  @throws[Exception] private def callJobsOverviewsApi(restUrl: String): Option[JobDetails] = {
    JobDetails.as(
      Request.get(s"$restUrl/jobs/overview")
        .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
        .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
        .execute.returnContent().asString(StandardCharsets.UTF_8)
    )
  }

  /**
   * Infer the current flink state from the last relevant k8s events.
   * This method is only used for application-mode job inference in
   * case of a failed JM rest request.
   */
  private def inferApplicationFlinkJobStateFromK8sEvent(@Nonnull trackId: TrackId)
                                                       (implicit pollEmitTime: Long): Option[JobStatusCV] = {

    // infer from k8s deployment and event
    val latest: JobStatusCV = trackController.jobStatuses.get(trackId)
    val jobState = {
      if (trackController.canceling.has(trackId)) FlinkJobState.CANCELED else {
        // whether deployment exists on kubernetes cluster
        val isDeployExists = KubernetesRetriever.isDeploymentExists(trackId.clusterId, trackId.namespace)
        if (isDeployExists) {
          FlinkJobState.K8S_INITIALIZING
        } else {
          val deployEvent = trackController.k8sDeploymentEvents.get(K8sEventKey(trackId.namespace, trackId.clusterId))
          if (deployEvent != null) {
            // no exists k8s deployment, infer from last deployment event
            val isDelete = deployEvent.action == Action.DELETED
            val isDeployAvailable = deployEvent.event.getStatus.getConditions.exists(_.getReason == "MinimumReplicasAvailable")
            (isDelete, isDeployAvailable) match {
              case (true, true) => FlinkJobState.POS_TERMINATED // maybe FINISHED or CANCEL
              case (true, false) => FlinkJobState.FAILED
              case _ => FlinkJobState.K8S_INITIALIZING
            }
          } else {
            // determine if the state should be SILENT or LOST
            inferSilentOrLostFromPreCache(latest)
          }
        }
      }
    }

    val jobStatusCV = JobStatusCV(
      jobState = jobState,
      jobId = null,
      pollEmitTime = pollEmitTime,
      pollAckTime = System.currentTimeMillis
    )

    if (jobState == FlinkJobState.SILENT && latest != null && latest.jobState == FlinkJobState.SILENT) {
      Some(jobStatusCV.copy(pollEmitTime = latest.pollEmitTime, pollAckTime = latest.pollAckTime))
    } else {
      Some(jobStatusCV)
    }
  }

  private[this] def inferSilentOrLostFromPreCache(preCache: JobStatusCV) = preCache match {
    case preCache if preCache == null => FlinkJobState.SILENT
    case preCache if preCache.jobState == FlinkJobState.SILENT &&
      System.currentTimeMillis() - preCache.pollAckTime >= conf.silentStateJobKeepTrackingSec * 1000 => FlinkJobState.LOST
    case _ => FlinkJobState.SILENT
  }

}

object FlinkJobStatusWatcher {

  private val effectEndStates: Seq[FlinkJobState.Value] = FlinkJobState.endingStates.filter(_ != FlinkJobState.LOST)

  /**
   * infer flink job state before persistence.
   * so drama, so sad.
   *
   * @param current  current flink job state
   * @param previous previous flink job state from persistent storage
   */
  def inferFlinkJobStateFromPersist(current: FlinkJobState.Value, previous: FlinkJobState.Value): FlinkJobState.Value = {
    current match {
      case FlinkJobState.LOST => if (effectEndStates.contains(current)) previous else FlinkJobState.TERMINATED
      case FlinkJobState.POS_TERMINATED | FlinkJobState.TERMINATED => previous match {
        case FlinkJobState.CANCELLING => FlinkJobState.CANCELED
        case FlinkJobState.FAILING => FlinkJobState.FAILED
        case _ => if (current == FlinkJobState.POS_TERMINATED) FlinkJobState.FINISHED else FlinkJobState.TERMINATED
      }
      case _ => current
    }
  }

}

private[kubernetes] case class JobDetails(jobs: Array[JobDetail] = Array())


private[kubernetes] case class JobDetail(jid: String,
                                         name: String,
                                         state: String,
                                         startTime: Long,
                                         endTime: Long,
                                         duration: Long,
                                         lastModification: Long,
                                         tasks: JobTask) {
  def toJobStatusCV(pollEmitTime: Long, pollAckTime: Long): JobStatusCV = {
    JobStatusCV(
      jobState = FlinkJobState.of(state),
      jobId = jid,
      jobName = name,
      jobStartTime = startTime,
      jobEndTime = endTime,
      duration = duration,
      taskTotal = tasks.total,
      pollEmitTime = pollEmitTime,
      pollAckTime = pollAckTime)
  }
}

private[kubernetes] case class JobTask(total: Int,
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


private[kubernetes] object JobDetails {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  def as(json: String): Option[JobDetails] = {

    Try(parse(json)) match {
      case Success(ok) =>
        ok \ "jobs" match {
          case JNothing | JNull => None
          case JArray(arr) =>
            val details = arr.map(x => {
              val task = x \ "tasks"
              JobDetail(
                (x \ "jid").extractOpt[String].orNull,
                (x \ "name").extractOpt[String].orNull,
                (x \ "state").extractOpt[String].orNull,
                (x \ "start-time").extractOpt[Long].getOrElse(0),
                (x \ "end-time").extractOpt[Long].getOrElse(0),
                (x \ "duration").extractOpt[Long].getOrElse(0),
                (x \ "last-modification").extractOpt[Long].getOrElse(0),
                JobTask(
                  (task \ "total").extractOpt[Int].getOrElse(0),
                  (task \ "created").extractOpt[Int].getOrElse(0),
                  (task \ "scheduled").extractOpt[Int].getOrElse(0),
                  (task \ "deploying").extractOpt[Int].getOrElse(0),
                  (task \ "running").extractOpt[Int].getOrElse(0),
                  (task \ "finished").extractOpt[Int].getOrElse(0),
                  (task \ "canceling").extractOpt[Int].getOrElse(0),
                  (task \ "canceled").extractOpt[Int].getOrElse(0),
                  (task \ "failed").extractOpt[Int].getOrElse(0),
                  (task \ "reconciling").extractOpt[Int].getOrElse(0),
                  (task \ "initializing").extractOpt[Int].getOrElse(0)
                )
              )
            }).toArray
            Some(JobDetails(details))
          case _ => None
        }
      case Failure(_) => None
    }

  }

}
