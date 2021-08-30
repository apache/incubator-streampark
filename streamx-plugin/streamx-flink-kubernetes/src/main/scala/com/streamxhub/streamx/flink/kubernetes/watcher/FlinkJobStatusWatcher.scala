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
import com.streamxhub.streamx.flink.kubernetes.model._
import com.streamxhub.streamx.flink.kubernetes.{FlinkTrkCachePool, KubernetesRetriever}
import io.fabric8.kubernetes.client.Watcher.Action
import org.apache.commons.collections.CollectionUtils
import org.apache.flink.runtime.client.JobStatusMessage

import java.util
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.concurrent.ThreadSafe
import javax.annotation.{Nonnull, Nullable}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
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
class FlinkJobStatusWatcher(cachePool: FlinkTrkCachePool,
                            conf: JobStatusWatcherConf = JobStatusWatcherConf.default) extends Logger with FlinkWatcher {

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
              // todo push trk jobStatue update event and remove trk cache record when necessary
              cachePool.jobStatuses.putAll(trkRs.toMap.asJava)
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
    tryWithResourceException(
      Try(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, SESSION))
        .getOrElse(return Array.empty[(TrkId, JobStatusCV)])) {
      flinkClient =>
        val jobDetailsFuture = flinkClient.listJobs()
        val jobDetails: util.Collection[JobStatusMessage] = jobDetailsFuture.get()
        val pollAckTime = System.currentTimeMillis
        // noinspection DuplicatedCode
        if (CollectionUtils.isNotEmpty(jobDetails)) {
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
        } else {
          Array.empty[(TrkId, JobStatusCV)]
        }
    } {
      exception =>
        logError(s"failed to list remote flink jobs on kubernetes-native-mode cluster, errorStack=${exception.getMessage}")
        Array.empty[(TrkId, JobStatusCV)]
    }
  }

  /**
   * Get flink status information from kubernetes-native-application cluster.
   * When the k8s-client or flink-cluster-client request fails or inferring
   * from k8s event fails, will return None.
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchApplicationJob(@Nonnull clusterId: String, @Nonnull namespace: String): Option[(TrkId, JobStatusCV)] = {
    val pollEmitTime = System.currentTimeMillis
    tryWithResourceException(
      Try(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, APPLICATION)).getOrElse(return None)) {
      flinkClient =>
        val jobDetailsFuture = flinkClient.listJobs()
        val jobDetails: util.Collection[JobStatusMessage] = jobDetailsFuture.get()
        val pollAckTime = System.currentTimeMillis
        if (CollectionUtils.isNotEmpty(jobDetails)) {
          // just receive the first result
          val jobStatusMsg = jobDetails.iterator().next()
          // noinspection DuplicatedCode
          Some(
            TrkId.onApplication(namespace, clusterId) -> JobStatusCV(
              jobState = FlinkJobState.of(jobStatusMsg.getJobState),
              jobId = jobStatusMsg.getJobId.toHexString,
              jobName = jobStatusMsg.getJobName,
              jobStartTime = jobStatusMsg.getStartTime,
              pollEmitTime = pollEmitTime,
              pollAckTime = pollAckTime)
          )
        } else {
          // when cannot found jobStatusMessage from flink-cluster-client, check last k8s event info
          val k8sEventKey = K8sEventKey(namespace, clusterId)
          val deploymentEvent = cachePool.k8sDeploymentEvents.getIfPresent(k8sEventKey)
          val jobState = inferFlinkJobStateFromK8sEvent(deploymentEvent)
          // ignore FlinkJobState.LOST
          jobState match {
            case FlinkJobState.LOST => None
            case _ => Some(TrkId.onApplication(namespace, clusterId) -> JobStatusCV(jobState, "", "", 0, pollEmitTime, pollAckTime))
          }
        }
    } {
      exception =>
        logError(s"failed to list remote flink jobs on kubernetes-application-mode cluster, errorStack=${exception.getMessage}")
        None
    }
  }

  /**
   * infer the current flink state from the last relevant k8s events
   */
  private def inferFlinkJobStateFromK8sEvent(@Nullable deployEvent: K8sDeploymentEventCV): FlinkJobState.Value = {
    if (deployEvent == null) {
      FlinkJobState.LOST
    } else {
      val isDelete = deployEvent.action == Action.DELETED
      val isDeployAvailable = deployEvent.event.getStatus.getConditions.exists(_.getReason == "MinimumReplicasAvailable")
      (isDelete, isDeployAvailable) match {
        case (true, true) => FlinkJobState.FINISHED
        case (true, false) => FlinkJobState.FAILED
        case _ => FlinkJobState.K8S_DEPLOYING
      }
    }
  }

}
