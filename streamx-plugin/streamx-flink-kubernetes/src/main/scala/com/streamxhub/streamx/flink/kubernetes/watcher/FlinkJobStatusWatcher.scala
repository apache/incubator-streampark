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
package com.streamxhub.streamx.flink.kubernetes.watcher

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.common.util.Utils.tryWithResourceException
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode.{APPLICATION, SESSION}
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobStatusChangeEvent
import com.streamxhub.streamx.flink.kubernetes.model._
import com.streamxhub.streamx.flink.kubernetes.{ChangeEventBus, FlinkTrkCachePool, JobStatusWatcherConf, KubernetesRetriever}
import io.fabric8.kubernetes.client.Watcher.Action
import org.apache.commons.collections.CollectionUtils
import org.apache.flink.runtime.client.JobStatusMessage

import java.util
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.Nonnull
import javax.annotation.concurrent.ThreadSafe
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.Try

/**
 * Watcher for continuously monitor flink job status on kubernetes-mode,
 * the traced flink identifiers from FlinkTrackCachePool.trackIds, the traced
 * result of flink jobs status would written to FlinkTrackCachePool.jobStatuses.
 *
 * auther:Al-assad
 */
@ThreadSafe
class FlinkJobStatusWatcher(conf: JobStatusWatcherConf = JobStatusWatcherConf.defaultConf)
                           (implicit val cachePool: FlinkTrkCachePool,
                            implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {

  private val trkTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trkTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trkTaskExecPool)

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
      timerSchedule = timerExec.scheduleAtFixedRate(() => trackingTask(), 0, conf.sglTrkTaskIntervalSec, TimeUnit.SECONDS)
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
    Try(trkTaskExecutor.shutdownNow())
    logInfo("[flink-k8s] FlinkJobStatusWatcher closed.")
  }

  /**
   * single flink job status tracking task
   */
  private def trackingTask(): Unit = {
    // get all legal tracking ids
    val trackIds: Set[TrkId] = Try(cachePool.collectDistinctTrackIds()).filter(_.nonEmpty).getOrElse(return)

    // retrieve flink job status in thread pool
    val tracksFuture: Set[Future[Array[(TrkId, JobStatusCV)]]] =
      trackIds.map(trkId => {
        val future = Future {
          trkId.executeMode match {
            case SESSION => touchSessionJob(trkId.clusterId, trkId.namespace)
            case APPLICATION => touchApplicationJob(trkId.clusterId, trkId.namespace).toArray
          }
        }
        future foreach {
          trkRs =>
            if (trkRs.nonEmpty) {
              implicit val preCache: mutable.Map[TrkId, JobStatusCV] = cachePool.jobStatuses.getAllPresent(trkRs.map(_._1).toSet.asJava).asScala
              // put job status to cache
              cachePool.jobStatuses.putAll(trkRs.toMap.asJava)
              // publish JobStatuChangeEvent when necessary
              trkRs.filter(e =>
                preCache.get(e._1) match {
                  case cv if cv.isEmpty => true
                  case cv if cv.get.jobState != e._2.jobState => true
                  case _ => false
                }).map(e => FlinkJobStatusChangeEvent(e._1, e._2))
                .foreach(eventBus.postSync)
              // remove trkId from cache of job that needs to be untracked
              trkRs.filter(r => FlinkJobState.isEndState(r._2.jobState))
                .map(_._1)
                .foreach(cachePool.trackIds.invalidate)
            }
        }
        future
      })
    // blocking until all future are completed or timeout is reached
    val allFutureHold = Future.sequence(tracksFuture)
    Try(
      Await.ready(allFutureHold, conf.sglTrkTaskIntervalSec seconds)
    ).failed.map(_ =>
      logError(s"[FlinkJobStatusWatcher] tracking flink job status on kubernetes mode timeout," +
        s" limitSeconds=${conf.sglTrkTaskIntervalSec}," +
        s" trackingIds=${trackIds.mkString(",")}"))
  }

  /**
   * Get flink status information from kubernetes-native-session cluster.
   * The empty array will returned when the k8s-client or flink-cluster-client
   * request fails.
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchSessionJob(@Nonnull clusterId: String, @Nonnull namespace: String): Array[(TrkId, JobStatusCV)] = {
    val pollEmitTime = System.currentTimeMillis
    lazy val defaultResult = Array.empty[(TrkId, JobStatusCV)]
    tryWithResourceException(
      Try(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, SESSION)).getOrElse(return defaultResult)) {
      flinkClient =>
        val jobDetailsFuture = flinkClient.listJobs()
        val jobDetails: util.Collection[JobStatusMessage] = jobDetailsFuture.get()
        val pollAckTime = System.currentTimeMillis
        if (CollectionUtils.isEmpty(jobDetails)) {
          defaultResult
        } else {
          jobDetails.asScala.map(e => (
            TrkId.onSession(namespace, clusterId, e.getJobId.toHexString),
            JobStatusCV(
              jobState = FlinkJobState.of(e.getJobState),
              jobId = e.getJobId.toHexString,
              jobName = e.getJobName,
              jobStartTime = e.getStartTime,
              pollEmitTime = pollEmitTime,
              pollAckTime = pollAckTime))
          ).toArray
        }
    } {
      exception =>
        logInfo(s"failed to list remote flink jobs on kubernetes-native-mode cluster, errorStack=${exception.getMessage}")
        defaultResult
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
  def touchApplicationJob(@Nonnull clusterId: String, @Nonnull namespace: String): Option[(TrkId, JobStatusCV)] = {
    implicit val pollEmitTime: Long = System.currentTimeMillis
    lazy val k8sInferResult = inferApplicationFlinkJobStateFromK8sEvent(clusterId, namespace)
    tryWithResourceException(
      Try(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, APPLICATION)).getOrElse(return k8sInferResult)) {
      flinkClient =>
        val jobDetailsFuture = flinkClient.listJobs()
        val jobDetails: util.Collection[JobStatusMessage] = jobDetailsFuture.get()
        if (CollectionUtils.isEmpty(jobDetails)) {
          k8sInferResult
        } else {
          // just receive the first result
          val jobStatusMsg = jobDetails.iterator().next()
          // noinspection DuplicatedCode
          Some(TrkId.onApplication(namespace, clusterId) -> JobStatusCV(
            jobState = FlinkJobState.of(jobStatusMsg.getJobState),
            jobId = jobStatusMsg.getJobId.toHexString,
            jobName = jobStatusMsg.getJobName,
            jobStartTime = jobStatusMsg.getStartTime,
            pollEmitTime = pollEmitTime,
            pollAckTime = System.currentTimeMillis))
        }
    }(exception => k8sInferResult)
  }

  /**
   * Infer the current flink state from the last relevant k8s events.
   * This method is only used for application-mode job inference in
   * case of a failed JM rest request.
   */
  private def inferApplicationFlinkJobStateFromK8sEvent(clusterId: String, namespace: String)
                                                       (implicit pollEmitTime: Long): Option[(TrkId, JobStatusCV)] = {

    // whether deployment exists on kubernetes cluster
    lazy val isDeployExists = KubernetesRetriever.isDeploymentExists(clusterId, namespace)
    // relavant deployment event
    lazy val deployEvent = cachePool.k8sDeploymentEvents.getIfPresent(K8sEventKey(namespace, clusterId))
    lazy val preCache = cachePool.jobStatuses.getIfPresent(TrkId.onApplication(namespace, clusterId))

    // infer from k8s deployment and event
    val jobState = {
      if (isDeployExists) {
        FlinkJobState.K8S_INITIALIZING
      } else if (deployEvent != null) {
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
        preCache match {
          case preCache if preCache == null => FlinkJobState.SILENT
          case preCache if preCache.jobState == FlinkJobState.SILENT &&
            System.currentTimeMillis() - preCache.pollAckTime >= conf.silentStateJobKeepTrackingSec * 1000 => FlinkJobState.LOST
          case _ => FlinkJobState.SILENT
        }
      }
    }
    if (jobState == FlinkJobState.SILENT && preCache != null && preCache.jobState == FlinkJobState.SILENT) {
      Some(TrkId.onApplication(namespace, clusterId) -> JobStatusCV(jobState, "", "", 0, preCache.pollEmitTime, preCache.pollAckTime))
    } else {
      Some(TrkId.onApplication(namespace, clusterId) -> JobStatusCV(jobState, "", "", 0, pollEmitTime, System.currentTimeMillis))
    }
  }

}

object FlinkJobStatusWatcher {

  private val effectEndStates: Seq[FlinkJobState.Value] = FlinkJobState.endingStates.filter(_ != FlinkJobState.LOST)

  /**
   * infer flink job state before persistence.
   *
   * @param curState current flink job state
   * @param preState previous flink job state from persistent storage
   */
  def inferFlinkJobStateFromPersist(curState: FlinkJobState.Value, preState: FlinkJobState.Value): FlinkJobState.Value = {
    if (curState == FlinkJobState.LOST) {
      if (effectEndStates.contains(curState)) {
        preState
      } else {
        FlinkJobState.TERMINATED
      }
    } else {
      curState
    }
  }

}
