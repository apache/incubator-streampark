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
import com.streamxhub.streamx.flink.kubernetes.event.FlinkClusterMetricChangeEvent
import com.streamxhub.streamx.flink.kubernetes.model.{ClusterKey, FlinkMetricCV}
import com.streamxhub.streamx.flink.kubernetes.{ChangeEventBus, FlinkTrkCachePool, KubernetesRetriever, MetricWatcherConf}
import org.apache.flink.configuration.{JobManagerOptions, MemorySize, TaskManagerOptions}
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.util.Timeout
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, JArray}

import java.nio.charset.StandardCharsets
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import javax.annotation.concurrent.ThreadSafe
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
 * auther:Al-assad
 */
//noinspection DuplicatedCode
@ThreadSafe
class FlinkMetricWatcher(conf: MetricWatcherConf = MetricWatcherConf.defaultConf)
                        (implicit val cachePool: FlinkTrkCachePool,
                         implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {


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
    // get all legal tracking cluster key
    val trkClusterKeys: Set[ClusterKey] = Try(cachePool.collectTrkClusterKeys()).filter(_.nonEmpty).getOrElse(return)

    // retrieve flink metrics in thread pool
    val trkFutures: Set[Future[Option[(ClusterKey, FlinkMetricCV)]]] =
      trkClusterKeys.map(clusterKey => {
        val future = Future(collectMetrics(clusterKey))
        future.filter(_.nonEmpty).foreach {
          result =>
            val (clusterKey, metric) = result.get._1 -> result.get._2
            // update current flink cluster metrics on cache
            cachePool.flinkMetrics.put(clusterKey, metric)
            val isMetricChanged = {
              val preMetric = cachePool.flinkMetrics.getIfPresent(clusterKey)
              preMetric == null || preMetric.nonEqualsPayload(metric)
            }
            if (isMetricChanged) eventBus.postAsync(FlinkClusterMetricChangeEvent(clusterKey, metric))
        }
        future
      })
    // blocking until all future are completed or timeout is reached
    Try {
      val futureHold = Future.sequence(trkFutures)
      Await.ready(futureHold, conf.sglTrkTaskTimeoutSec seconds)
    }.failed.map { _ =>
      logError(s"[FlinkMetricWatcher] tracking flink metrics on kubernetes mode timeout," +
        s" limitSeconds=${conf.sglTrkTaskTimeoutSec}," +
        s" trackingClusterKeys=${trkClusterKeys.mkString(",")}")
    }
  }

  /**
   * Collect flink runtime metrics from kubernetes-native cluster.
   * Returns None when the flink-cluster-client request fails (or
   * in case of the relevant flink rest api require failure).
   *
   * This method can be called directly from outside, without affecting the
   * current cachePool result.
   */
  def collectMetrics(clusterKey: ClusterKey): Option[(ClusterKey, FlinkMetricCV)] = {
    // get flink rest api
    val flinkJmRestUrl = cachePool.getClusterRestUrl(clusterKey).filter(_.nonEmpty).getOrElse(return None)

    // call flink rest overview api
    val flinkOverview: FlinkRestOverview = Try(
      FlinkRestOverview.as(
        Request.get(s"$flinkJmRestUrl/overview")
          .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
          .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
          .execute.returnContent.asString(StandardCharsets.UTF_8)
      )
    ).getOrElse(return None)


    // call flink rest jm config api

    val flinkJmConfigs = Try(
      FlinkRestJmConfigItem
        .as(Request.get(s"$flinkJmRestUrl/jobmanager/config")
          .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
          .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
          .execute.returnContent.asString(StandardCharsets.UTF_8)
        ).map(e => (e.key, e.value))
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
        finishedJob = flinkOverview.jobsFinished,
        cancelledJob = flinkOverview.jobsCancelled,
        failedJob = flinkOverview.jobsFailed,
        pollAckTime = ackTime)
    }
    Some(clusterKey -> flinkMetricCV)
  }

}

/**
 * bean for response message of flink-rest/overview
 */
private[kubernetes] case class FlinkRestOverview(taskManagers: Integer,
                                                 slotsTotal: Integer,
                                                 slotsAvailable: Integer,
                                                 jobsRunning: Integer,
                                                 jobsFinished: Integer,
                                                 jobsCancelled: Integer,
                                                 jobsFailed: Integer,
                                                 flinkVersion: String)

object FlinkRestOverview {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  def as(json: String): FlinkRestOverview = {
    Try(parse(json)) match {
      case Success(ok) =>
        FlinkRestOverview(
          (ok \ "taskmanagers").extractOpt[Integer].getOrElse(0),
          (ok \ "slots-total").extractOpt[Integer].getOrElse(0),
          (ok \ "slots-available").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-running").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-finished").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-cancelled").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-failed").extractOpt[Integer].getOrElse(0),
          (ok \ "flink-version").extractOpt[String].orNull
        )
      case Failure(_) => null
    }
  }

}

/**
 * bean for response message of flink-rest/jobmanager/config
 */
private[kubernetes] case class FlinkRestJmConfigItem(key: String, value: String)

private[kubernetes] object FlinkRestJmConfigItem {

  @transient
  implicit lazy val formats: DefaultFormats.type = org.json4s.DefaultFormats

  def as(json: String): List[FlinkRestJmConfigItem] = {
    Try(parse(json)) match {
      case Success(ok) =>
        ok match {
          case JArray(arr) =>
            arr.map(x => {
              FlinkRestJmConfigItem(
                (x \ "key").extractOpt[String].orNull,
                (x \ "value").extractOpt[String].orNull
              )
            })
          case _ => null
        }
      case Failure(_) => null
    }
  }

}
