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

package org.apache.streampark.flink.kubernetes.watcher

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.kubernetes.event.FlinkClusterMetricChangeEvent
import org.apache.streampark.flink.kubernetes.model.{ClusterKey, FlinkMetricCV, TrackId}
import org.apache.streampark.flink.kubernetes.{ChangeEventBus, FlinkTrackController, KubernetesRetriever, MetricWatcherConfig}
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

@ThreadSafe
class FlinkMetricWatcher(conf: MetricWatcherConfig = MetricWatcherConfig.defaultConf)
                        (implicit val trackController: FlinkTrackController,
                         implicit val eventBus: ChangeEventBus) extends Logger with FlinkWatcher {


  private val trackTaskExecPool = Executors.newWorkStealingPool()
  private implicit val trackTaskExecutor: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(trackTaskExecPool)

  private val timerExec = Executors.newSingleThreadScheduledExecutor()
  private var timerSchedule: ScheduledFuture[_] = _

  /**
   * start watcher process
   */
  override def doStart(): Unit = {
    timerSchedule = timerExec.scheduleAtFixedRate(() => doWatch(), 0, conf.requestIntervalSec, TimeUnit.SECONDS)
    logInfo("[flink-k8s] FlinkMetricWatcher started.")
  }

  /**
   * stop watcher process
   */
  override def doStop(): Unit = {
    timerSchedule.cancel(true)
    logInfo("[flink-k8s] FlinkMetricWatcher stopped.")
  }

  /**
   * closes resource, relinquishing any underlying resources.
   */
  override def doClose(): Unit = {
    timerExec.shutdownNow()
    trackTaskExecutor.shutdownNow()
    logInfo("[flink-k8s] FlinkMetricWatcher closed.")
  }

  /**
   * single flink metrics tracking task
   */
  override def doWatch(): Unit = {
    // get all legal tracking cluster key
    val trackIds: Set[TrackId] = Try(trackController.collectTracks()).filter(_.nonEmpty).getOrElse(return)
    // retrieve flink metrics in thread pool
    val futures: Set[Future[Option[FlinkMetricCV]]] =
      trackIds.map(id => {
        val future = Future(collectMetrics(id))
        future onComplete (_.getOrElse(None) match {
          case Some(metric) =>
            val clusterKey = id.toClusterKey
            // update current flink cluster metrics on cache
            trackController.flinkMetrics.put(clusterKey, metric)
            val isMetricChanged = {
              val preMetric = trackController.flinkMetrics.get(clusterKey)
              preMetric == null || !preMetric.equalsPayload(metric)
            }
            if (isMetricChanged) {
              eventBus.postAsync(FlinkClusterMetricChangeEvent(id, metric))
            }
          case _ =>
        })
        future
      })
    // blocking until all future are completed or timeout is reached
    Try(Await.ready(Future.sequence(futures), conf.requestTimeoutSec seconds))
      .failed.map { _ =>
      logError(s"[FlinkMetricWatcher] tracking flink metrics on kubernetes mode timeout," +
        s" limitSeconds=${conf.requestTimeoutSec}," +
        s" trackingClusterKeys=${trackIds.mkString(",")}")
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
  def collectMetrics(id: TrackId): Option[FlinkMetricCV] = {
    // get flink rest api
    val clusterKey: ClusterKey = ClusterKey.of(id)
    val flinkJmRestUrl = trackController.getClusterRestUrl(clusterKey).filter(_.nonEmpty).getOrElse(return None)

    // call flink rest overview api
    val flinkOverview: FlinkRestOverview = FlinkRestOverview.as(
      Request.get(s"$flinkJmRestUrl/overview")
        .connectTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_REST_AWAIT_TIMEOUT_SEC))
        .responseTimeout(Timeout.ofSeconds(KubernetesRetriever.FLINK_CLIENT_TIMEOUT_SEC))
        .execute.returnContent.asString(StandardCharsets.UTF_8)
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
    Some(flinkMetricCV)
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

  def as(json: String): Option[FlinkRestOverview] = {
    Try(parse(json)) match {
      case Success(ok) =>
        val overview = FlinkRestOverview(
          (ok \ "taskmanagers").extractOpt[Integer].getOrElse(0),
          (ok \ "slots-total").extractOpt[Integer].getOrElse(0),
          (ok \ "slots-available").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-running").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-finished").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-cancelled").extractOpt[Integer].getOrElse(0),
          (ok \ "jobs-failed").extractOpt[Integer].getOrElse(0),
          (ok \ "flink-version").extractOpt[String].orNull
        )
        Some(overview)
      case Failure(_) => None
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
