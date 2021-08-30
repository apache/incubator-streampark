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

import com.fasterxml.jackson.annotation.JsonProperty
import com.streamxhub.streamx.common.util.JsonUtils.Unmarshal
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode
import com.streamxhub.streamx.flink.kubernetes.model.{FlinkMetricCV, TrkId}
import com.streamxhub.streamx.flink.kubernetes.{FlinkTrkCachePool, KubernetesRetriever, MetricWatcherConf}
import org.apache.flink.configuration.{JobManagerOptions, MemorySize, TaskManagerOptions}
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.util.Timeout

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import java.util.function.UnaryOperator
import javax.annotation.Nonnull
import javax.annotation.concurrent.ThreadSafe
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps
import scala.util.Try

/**
 * auther:Al-assad
 */
@ThreadSafe
class FlinkMetricWatcher(cachePool: FlinkTrkCachePool,
                         conf: MetricWatcherConf = MetricWatcherConf.default) extends Logger with FlinkWatcher {

  private val trkTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trkTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trkTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  // status of whether FlinkJobWatcher has already started
  @volatile private var isStarted = false

  /**
   * start watcher process
   */
  // noinspection DuplicatedCode
  override def start(): Unit = this.synchronized {
    if (!isStarted) {
      timerSchedule = timerExec.scheduleAtFixedRate(() => trackingTask(), 0, conf.sglTrkTaskIntervalSec, TimeUnit.SECONDS)
      isStarted = true
      logInfo("[flink-k8s] FlinkMetricWatcher started.")
    }
  }

  /**
   * stop watcher process
   */
  override def stop(): Unit = this.synchronized {
    if (isStarted) {
      timerSchedule.cancel(true)
      isStarted = false
      logInfo("[flink-k8s] FlinkMetricWatcher stopped.")
    }
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  // noinspection DuplicatedCode
  override def close(): Unit = this.synchronized {
    if (isStarted) {
      timerSchedule.cancel(true)
      isStarted = false
    }
    Try(timerExec.shutdownNow())
    Try(trkTaskExecutor.shutdownNow())
    logInfo("[flink-k8s] FlinkMetricWatcher closed.")
  }

  /**
   * single flink metrics tracking task
   */
  private def trackingTask(): Unit = {
    // get all legal tracking ids
    val trkIds: Set[TrkId] = Try(cachePool.collectDistinctTrackIds()).filter(_.nonEmpty).getOrElse(return)
    val accMetrics = new AtomicReference[FlinkMetricCV](FlinkMetricCV.empty)

    // retrieve flink metrics in thread pool
    val trkFutures: Set[Future[Option[FlinkMetricCV]]] =
      trkIds.map(id => {
        val future = Future(collectMetrics(id.executeMode, id.clusterId, id.namespace))
        future.foreach { metric =>
          if (metric.nonEmpty) {
            accMetrics.updateAndGet(new UnaryOperator[FlinkMetricCV] {
              override def apply(t: FlinkMetricCV): FlinkMetricCV = t + metric.get
            })
          }
        }
        future
      })
    // blocking until all future are completed or timeout is reached
    val futureHold = Future.sequence(trkFutures)
    Try({
      Await.ready(futureHold, conf.sglTrkTaskIntervalSec seconds)
      // write metrics to cache
      cachePool.flinkMetrics.set(accMetrics.get)
    }).failed.map(_ =>
      logError(s"[FlinkMetricWatcher] tracking flink metrics on kubernetes mode timeout," +
        s" limitSeconds=${conf.sglTrkTaskIntervalSec}," +
        s" trackingIds=${trkIds.mkString(",")}"))
  }

  /**
   * Collect flink runtime metrics from kubernetes-native cluster.
   * Returns None when the flink-cluster-client request fails (or
   * in case of the relevant flink rest api require failure).
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def collectMetrics(@Nonnull executeMode: FlinkK8sExecuteMode.Value,
                     @Nonnull clusterId: String,
                     @Nonnull namespace: String): Option[FlinkMetricCV] = {
    // get flink web interface url
    val flinkJmRestUrl = Try(KubernetesRetriever.newFinkClusterClient(clusterId, namespace, executeMode).getWebInterfaceURL)
      .filter(_.nonEmpty).getOrElse(return None)

    // call flink rest overview api
    val flinkOverview: FlinkRestOverview = Try(
      Request.get(s"$flinkJmRestUrl/overview")
        .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
        .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
        .execute.returnContent.asString(StandardCharsets.UTF_8)
        .fromJson[FlinkRestOverview])
      .getOrElse(return None)

    // call flink rest jm config api
    val flinkJmConfigs = Try(
      Request.get(s"$flinkJmRestUrl/jobmanager/config")
        .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
        .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
        .execute.returnContent.asString(StandardCharsets.UTF_8)
        .fromJson[Array[FlinkRestJmConfigItem]]
        .map(e => (e.key, e.value))
        .toMap
    ).getOrElse(return None)

    val ackTime = System.currentTimeMillis
    val flinkMetricCV = {
      val tmMemStr = flinkJmConfigs.getOrElse(TaskManagerOptions.TOTAL_PROCESS_MEMORY.key, "0b")
      val jmMemStr = flinkJmConfigs.getOrElse(JobManagerOptions.TOTAL_PROCESS_MEMORY.key, "0b")
      FlinkMetricCV(
        totalJmMemory = MemorySize.parse(jmMemStr).getMebiBytes,
        totalTmMemory = MemorySize.parse(tmMemStr).getMebiBytes * flinkOverview.taskManagers,
        totalTm = flinkOverview.taskManagers,
        totalSlot = flinkOverview.slotsTotal,
        availableSlot = flinkOverview.slotsAvailable,
        runningJob = flinkOverview.jobsRunning,
        pollAckTime = ackTime)
    }
    Some(flinkMetricCV)
  }

}

/**
 * bean for response message of flink-rest/overview
 */
private[kubernetes] case class FlinkRestOverview(@JsonProperty("taskmanagers") taskManagers: Integer,
                                                 @JsonProperty("slots-total") slotsTotal: Integer,
                                                 @JsonProperty("slots-available") slotsAvailable: Integer,
                                                 @JsonProperty("jobs-running") jobsRunning: Integer,
                                                 @JsonProperty("jobs-finished") jobsFinished: Integer,
                                                 @JsonProperty("jobs-cancelled") jobsCancelled: Integer,
                                                 @JsonProperty("jobs-failed") jobsFailed: Integer,
                                                 @JsonProperty("flink-version") flinkVersion: String)

/**
 * bean for response message of flink-rest/jobmanager/config
 */
private[kubernetes] case class FlinkRestJmConfigItem(@JsonProperty("key") key: String,
                                                     @JsonProperty("value") value: String)
