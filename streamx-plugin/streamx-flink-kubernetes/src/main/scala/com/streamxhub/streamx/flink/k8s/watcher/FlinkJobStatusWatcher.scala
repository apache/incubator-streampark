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
package com.streamxhub.streamx.flink.k8s.watcher

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.common.util.Utils.tryWithResourceExc
import com.streamxhub.streamx.flink.k8s.enums.FlinkJobState
import com.streamxhub.streamx.flink.k8s.enums.FlinkK8sExecuteMode.{APPLICATION, SESSION}
import com.streamxhub.streamx.flink.k8s.model._
import com.streamxhub.streamx.flink.k8s.{FlinkTRKCachePool, FlinkWatcher, KubernetesRetriever}
import io.fabric8.kubernetes.client.Watcher.Action
import org.apache.commons.collections.CollectionUtils
import org.apache.flink.runtime.client.JobStatusMessage

import java.util
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.Nullable
import javax.annotation.concurrent.ThreadSafe
import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Success, Try}

/**
 * Watcher for continuously monitor flink job status on kubernetes-mode,
 * the traced flink identifiers from FlinkTRKCachePool.trkIds, the traced
 * result of flink jobs status would written to FlinkTRKCachePool.jobStatuses.
 *
 * auther:Al-assad
 */
@ThreadSafe
class FlinkJobStatusWatcher(cachePool: FlinkTRKCachePool,
                            conf: FlinkJobStatusWatcherConf = FlinkJobStatusWatcherConf()) extends Logger with FlinkWatcher {

  private val trkTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trkTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trkTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  // status of whether FlinkJobWatcher has already started
  @volatile private var isStarted = false

  /**
   * stop watcher process
   */
  override def start(): Unit = this.synchronized {
    if (!isStarted) {
      timerSchedule = timerExec.scheduleAtFixedRate(() => trackingTask(), 0, conf.sglTrkTaskIntervalSec, TimeUnit.SECONDS)
      isStarted = true
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
    }
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  override def close(): Unit = this.synchronized {
    if (isStarted) {
      timerSchedule.cancel(true)
      isStarted = false
    }
    Try(timerExec.shutdownNow())
    Try(trkTaskExecutor.shutdownNow())
  }

  /**
   * single flink job status tracking task
   */
  private def trackingTask(): Unit = {
    val trkIds: Set[TrkId] = cachePool.collectAllTrkIds()
    // remove jobId info of session mode trkId
    val distinctTrkIds = trkIds
      .map(trkId => if (trkId.executeMode == SESSION) TrkId(trkId.executeMode, trkId.namespace, trkId.clusterId, "") else trkId)
      .filter(_.isLegal)
    // retieve flink job status in thread pool
    val trksFuture = distinctTrkIds.map(trkId => {
      val future = Future {
        trkId.executeMode match {
          case SESSION => touchSessionJob(trkId.clusterId, trkId.namespace)
          case APPLICATION => touchApplicationJob(trkId.clusterId, trkId.namespace).toArray
        }
      }
      future onComplete {
        case Success(results) => if (!results.isEmpty) {
          // todo push trk jobStatue update event and remove trk cache record when necessary
          cachePool.jobStatuses.putAll(results.toMap.asJava)
        }
      }
      future
    })
    // blocking until all future completes ir timeout
    val allFutureHold = Future.sequence(trksFuture)
    Try(Await.ready(allFutureHold, conf.sglTrkTaskIntervalSec seconds)).map(_ => logError(
      s"[FlinkJobWatcher] tracking flink job status on kubernetes mode timeout," +
        s" limitSeconds=${conf.sglTrkTaskIntervalSec}," +
        s" trackingIds=${distinctTrkIds.mkString(",")}"))
  }

  /**
   * Get flink status information from kubernetes-native-session cluster.
   * The empty array will returned when the k8s-client or flink-cluster-client
   * request fails.
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchSessionJob(clusterId: String, namespace: String): Array[(TrkId, JobStatusCV)] = {
    val pollEmitTime = System.currentTimeMillis
    tryWithResourceExc(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, SESSION)) {
      flinkClient =>
        val jobDetailsFuture = flinkClient.listJobs()
        val jobDetails: util.Collection[JobStatusMessage] = jobDetailsFuture.get()
        val pollAckTime = System.currentTimeMillis
        if (CollectionUtils.isNotEmpty(jobDetails)) {
          jobDetails.asScala.map(e => (
            TrkId.onSession(namespace, clusterId, e.getJobId.toHexString),
            JobStatusCV(
              FlinkJobState.of(e.getJobState),
              e.getJobId.toHexString,
              e.getJobName,
              e.getStartTime,
              pollEmitTime,
              pollAckTime))
          ).toArray
        } else {
          Array.empty[(TrkId, JobStatusCV)]
        }
    } {
      exception =>
        logError(s"failed to list remote flink jobs on kubernetes-native-mode cluster, errorStack=${exception.getMessage}")
        Array.empty[(TrkId, JobStatusCV)]
    }.asInstanceOf[Array[(TrkId, JobStatusCV)]]
  }

  /**
   * Get flink status information from kubernetes-native-application cluster.
   * When the k8s-client or flink-cluster-client request fails or inferring
   * from k8s event fails, will return None.
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def touchApplicationJob(clusterId: String, namespace: String): Option[(TrkId, JobStatusCV)] = {
    val pollEmitTime = System.currentTimeMillis
    tryWithResourceExc(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, APPLICATION)) {
      flinkClient =>
        val jobDetailsFuture = flinkClient.listJobs()
        val jobDetails: util.Collection[JobStatusMessage] = jobDetailsFuture.get()
        val pollAckTime = System.currentTimeMillis
        if (CollectionUtils.isNotEmpty(jobDetails)) {
          // just receive the first result
          val jobStatusMsg = jobDetails.iterator().next()
          Some(
            TrkId.onApplication(namespace, clusterId) -> JobStatusCV(
              FlinkJobState.of(jobStatusMsg.getJobState),
              jobStatusMsg.getJobId.toHexString,
              jobStatusMsg.getJobName,
              jobStatusMsg.getStartTime,
              pollEmitTime,
              pollAckTime)
          )
        } else {
          // when cannot found jobStatusMessage from flink-cluster-client, check last k8s event info
          val k8sEventKey = K8sEventKey(namespace, clusterId)
          val deploymentEvent = cachePool.k8sDeploymentEvents.getIfPresent(k8sEventKey)
          val jobState = inferFlinkJobStateFromK8sEvent(deploymentEvent)
          // ignore FlinkJobState.LOST
          if (jobState == FlinkJobState.LOST) None
          else Some(TrkId.onApplication(namespace, clusterId) -> JobStatusCV(jobState, "", "", 0, pollEmitTime, pollAckTime))
        }
    } {
      exception =>
        logError(s"failed to list remote flink jobs on kubernetes-application-mode cluster, errorStack=${exception.getMessage}")
        None
    }.asInstanceOf[Option[(TrkId, JobStatusCV)]]
  }

  /**
   * infer the current flink state from the last relevant k8s events
   */
  private def inferFlinkJobStateFromK8sEvent(@Nullable deployEvent: K8sDeploymentEventCV): FlinkJobState.Value = {
    if (deployEvent != null) {
      val isDelete = deployEvent.action == Action.DELETED
      val isDeployAvaliable = deployEvent.event.getStatus.getConditions.asScala.exists(_.getReason == "MinimumReplicasAvailable")
      () match {
        case _ if isDelete && isDeployAvaliable => FlinkJobState.FINISHED
        case _ if isDelete && !isDeployAvaliable => FlinkJobState.FAILED
        case _ => FlinkJobState.K8S_DEPLOYING
      }
    } else {
      FlinkJobState.LOST
    }
  }

}
