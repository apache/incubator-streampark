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
import com.streamxhub.streamx.flink.kubernetes.{ChangeEventBus, FlinkTrackCachePool, IngressController, JobStatusWatcherConfig, KubernetesRetriever}
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.util.Timeout
import org.json4s.{DefaultFormats, JNothing, JNull}
import org.json4s.JsonAST.JArray
import org.json4s.jackson.JsonMethods.parse

import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.Nonnull
import javax.annotation.concurrent.ThreadSafe
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
                           (implicit val cachePool: FlinkTrackCachePool,
                            implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {

  private val trackTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trackTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trackTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  // status of whether FlinkJobWatcher has already started
  @volatile private var isStarted = false

  /**
   * stop watcher process
   */
  //noinspection DuplicatedCode
  override def start(): Unit = this.synchronized {
    if (!isStarted) {
      timerSchedule = timerExec.scheduleAtFixedRate(() => watch(), 0, conf.requestIntervalSec, TimeUnit.SECONDS)
      isStarted = true
      logInfo("[flink-k8s] FlinkJobStatusWatcher started.")
    }
  }

  /**
   * stop watcher process
   */
  override def stop(): Unit = this.synchronized {
    if (isStarted) {
      // interrupt all running threads
      timerSchedule.cancel(true)
      isStarted = false
      logInfo("[flink-k8s] FlinkJobStatusWatcher stopped.")
    }
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  //noinspection DuplicatedCode
  override def close(): Unit = this.synchronized {
    if (isStarted) {
      timerSchedule.cancel(true)
      isStarted = false
    }
    Try(timerExec.shutdownNow())
    Try(trackTaskExecutor.shutdownNow())
    logInfo("[flink-k8s] FlinkJobStatusWatcher closed.")
  }

  /**
   * single flink job status tracking task
   */
  override def watch(): Unit = {
    // get all legal tracking ids
    val trackIds = Try(cachePool.collectAllTrackIds()).filter(_.nonEmpty).getOrElse(return)

    // retrieve flink job status in thread pool
    val tracksFuture: Set[Future[Option[(TrackId, JobStatusCV)]]] =
      trackIds.map(trackId => {
        val future = Future {
          trackId.executeMode match {
            case SESSION =>
              touchSessionJob(trackId.clusterId, trackId.namespace, trackId.appId, trackId.jobId)
            case APPLICATION =>
              touchApplicationJob(trackId.clusterId, trackId.namespace, trackId.appId)
          }
        }

        future.filter(_.nonEmpty).foreach { tracks =>

          val preCache: Map[TrackId, JobStatusCV] = cachePool.jobStatuses.getAsMap(tracks.map(_._1).toSet)
          // put job status to cache
          cachePool.jobStatuses.putAll(tracks.toMap)
          // set jobId to trackIds
          tracks.foreach(x => cachePool.trackIds.update(x._1.copy(jobId = x._2.jobId)))

          // publish JobStatusChangeEvent when necessary
          tracks.filter(e => {
            preCache.get(e._1) match {
              case Some(pre) => pre.jobState != e._2.jobState
              case _ => FlinkJobState.isEndState(e._2.jobState)
            }
          }).map(e => FlinkJobStatusChangeEvent(e._1, e._2))
            .foreach(eventBus.postSync)

          // remove trackId from cache of job that needs to be untracked
          tracks
            .filter(r => FlinkJobState.isEndState(r._2.jobState))
            .foreach { x =>
              cachePool.trackIds.invalidate(x._1)
              if (x._1.executeMode == APPLICATION) {
                cachePool.endpoints.invalidate(x._1.toClusterKey)
              }
            }
        }

        future
      })

    // blocking until all future are completed or timeout is reached
    val allFutureHold = Future.sequence(tracksFuture)
    Try(Await.ready(allFutureHold, conf.requestTimeoutSec seconds)).failed.map(_ =>
      logInfo(s"[FlinkJobStatusWatcher] tracking flink job status on kubernetes mode timeout," +
        s" limitSeconds=${conf.requestTimeoutSec}," +
        s" trackIds=${trackIds.mkString(",")}"))
  }

  /**
   * Get flink status information from kubernetes-native-session cluster.
   * When the flink-cluster-client request fails, the job state would be
   * LOST or SILENT.
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchSessionJob(@Nonnull clusterId: String,
                      @Nonnull namespace: String,
                      @Nonnull appId: Long,
                      @Nonnull jobId: String): Option[(TrackId, JobStatusCV)] = {
    val pollEmitTime = System.currentTimeMillis
    val rsMap = touchSessionAllJob(clusterId, namespace, appId).toMap
    val id = TrackId.onSession(namespace, clusterId, appId, jobId)
    val jobState = rsMap.get(id).filter(_.jobState != FlinkJobState.SILENT).getOrElse {
      val preCache = cachePool.jobStatuses.get(id)
      val state = inferSilentOrLostFromPreCache(preCache)
      val nonFirstSilent = state == FlinkJobState.SILENT && preCache != null && preCache.jobState == FlinkJobState.SILENT
      if (nonFirstSilent) {
        JobStatusCV(jobState = state, jobId = id.jobId, pollEmitTime = preCache.pollEmitTime, pollAckTime = preCache.pollAckTime)
      } else {
        JobStatusCV(jobState = state, jobId = id.jobId, pollEmitTime = pollEmitTime, pollAckTime = System.currentTimeMillis)
      }
    }
    Some(id -> jobState)
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
  def touchApplicationJob(@Nonnull clusterId: String,
                          @Nonnull namespace: String,
                          @Nonnull appId: Long
                         ): Option[(TrackId, JobStatusCV)] = {
    implicit val pollEmitTime: Long = System.currentTimeMillis
    lazy val k8sInferResult = inferApplicationFlinkJobStateFromK8sEvent(clusterId, namespace, appId)
    val jobDetails = listJobsDetails(ClusterKey(APPLICATION, namespace, clusterId)).getOrElse(return k8sInferResult).jobs
    if (jobDetails.isEmpty) {
      k8sInferResult
    } else {
      // just receive the first result
      val jobDetail = jobDetails.iterator.next.toJobStatusCV(pollEmitTime, System.currentTimeMillis)
      val trackId = TrackId.onApplication(namespace, clusterId, appId)
      Some(trackId.copy(jobId = jobDetail.jobId) -> jobDetail)
    }
  }

  /**
   * list flink jobs details
   */
  private def listJobsDetails(clusterKey: ClusterKey): Option[JobDetails] = {
    // get flink rest api
    var clusterRestUrl = cachePool.getClusterRestUrl(clusterKey).filter(_.nonEmpty).getOrElse(return None)
    // list flink jobs from rest api
    Try(callJobsOverviewsApi(clusterRestUrl))
      .recover { case _ =>
        clusterRestUrl = cachePool.refreshClusterRestUrl(clusterKey).getOrElse(return None)
        callJobsOverviewsApi(clusterRestUrl)
      }
      .recover { case ex =>
        logInfo(s"failed to list remote flink jobs on kubernetes-native-mode cluster, errorStack=${ex.getMessage}")
        IngressController.deleteIngress(clusterKey.clusterId, clusterKey.namespace)
        return None
      }
      .toOption
  }

  /**
   * list flink jobs details from rest api
   */
  @throws[Exception] private def callJobsOverviewsApi(restUrl: String): JobDetails = JobDetails.as(
    Request.get(s"$restUrl/jobs/overview")
      .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
      .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
      .execute.returnContent().asString(StandardCharsets.UTF_8)
  ).getOrElse(null)

  /**
   * Infer the current flink state from the last relevant k8s events.
   * This method is only used for application-mode job inference in
   * case of a failed JM rest request.
   */
  private def inferApplicationFlinkJobStateFromK8sEvent(@Nonnull clusterId: String,
                                                        @Nonnull namespace: String,
                                                        appId: Long)
                                                       (implicit pollEmitTime: Long): Option[(TrackId, JobStatusCV)] = {

    // whether deployment exists on kubernetes cluster
    lazy val isDeployExists = KubernetesRetriever.isDeploymentExists(clusterId, namespace)
    // relevant deployment event
    lazy val deployEvent = cachePool.k8sDeploymentEvents.get(K8sEventKey(namespace, clusterId))
    lazy val preCache: JobStatusCV = cachePool.jobStatuses.get(TrackId.onApplication(namespace, clusterId, appId))

    // infer from k8s deployment and event
    val jobState = {
      if (isDeployExists) {
        FlinkJobState.K8S_INITIALIZING
      } else {
        FlinkJobState.FAILED
      }
    }

    val nonFirstSilent = jobState == FlinkJobState.SILENT && preCache != null && preCache.jobState == FlinkJobState.SILENT
    if (nonFirstSilent) {
      Some(
        TrackId.onApplication(namespace, clusterId, appId) ->
          JobStatusCV(
            jobState = jobState,
            jobId = null,
            pollEmitTime = preCache.pollEmitTime,
            pollAckTime = preCache.pollAckTime)
      )
    } else {
      Some(
        TrackId.onApplication(namespace, clusterId, appId) ->
          JobStatusCV(
            jobState = jobState,
            jobId = null,
            pollEmitTime = pollEmitTime,
            pollAckTime = System.currentTimeMillis
          )
      )
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
   * @param curState current flink job state
   * @param preState previous flink job state from persistent storage
   */
  def inferFlinkJobStateFromPersist(curState: FlinkJobState.Value, preState: FlinkJobState.Value): FlinkJobState.Value =
    curState match {
      case FlinkJobState.LOST =>
        if (effectEndStates.contains(curState)) preState
        else FlinkJobState.TERMINATED
      case FlinkJobState.POS_TERMINATED => preState match {
        case FlinkJobState.CANCELLING => FlinkJobState.CANCELED
        case FlinkJobState.FAILING => FlinkJobState.FAILED
        case _ => FlinkJobState.FINISHED
      }
      case FlinkJobState.TERMINATED => preState match {
        case FlinkJobState.CANCELLING => FlinkJobState.CANCELED
        case FlinkJobState.FAILING => FlinkJobState.FAILED
        case _ => FlinkJobState.TERMINATED
      }
      case _ => curState
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
