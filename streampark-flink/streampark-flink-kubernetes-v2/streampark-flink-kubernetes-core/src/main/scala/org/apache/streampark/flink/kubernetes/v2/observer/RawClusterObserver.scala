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

package org.apache.streampark.flink.kubernetes.v2.observer

import org.apache.streampark.common.zio.ZIOExt.{OptionZIOOps, UIOOps}
import org.apache.streampark.flink.kubernetes.v2.{FlinkMemorySizeParser, FlinkRestRequest}
import org.apache.streampark.flink.kubernetes.v2.model._

import zio.{Fiber, Schedule, UIO, ZIO}
import zio.concurrent.ConcurrentMap
import zio.stream.ZStream

import scala.util.Try

/** Observer for Flink cluster REST API. */
case class RawClusterObserver(
    restSvcEndpointSnaps: ConcurrentMap[(Namespace, Name), RestSvcEndpoint],
    clusterJobStatusSnaps: ConcurrentMap[(Namespace, Name), Vector[JobStatus]],
    clusterMetricsSnaps: ConcurrentMap[(Namespace, Name), ClusterMetrics]) {

  private val jobOverviewPollFibers    = ConcurrentMap.empty[(Namespace, Name), Fiber.Runtime[_, _]].runUIO
  private val clusterMetricsPollFibers = ConcurrentMap.empty[(Namespace, Name), Fiber.Runtime[_, _]].runUIO

  def watch(namespace: String, name: String): UIO[Unit] = {
    watchJobOverviews(namespace, name) *>
    watchClusterMetrics(namespace, name)
  }

  def unWatch(namespace: String, name: String): UIO[Unit] = {
    unWatchJobOverviews(namespace, name) *>
    unWatchClusterMetrics(namespace, name)
  }

  /** Monitor Flink job overview API. */
  private def watchJobOverviews(namespace: String, name: String): UIO[Unit] = {

    val procEffect = ZStream
      // retrieve rest endpoint
      .fromZIO(
        restSvcEndpointSnaps
          .get(namespace, name)
          .flatMap {
            case None           => ZIO.fail(RestEndpointNotFound)
            // request job overview api
            case Some(endpoint) => FlinkRestRequest(endpoint.chooseRest).listJobOverviewInfo
          })
      .retry(Schedule.spaced(restRetryInterval))
      .map(jobOverviews => jobOverviews.map(info => JobStatus.fromRest(info)))
      .tap(jobStatuses => clusterJobStatusSnaps.put((namespace, name), jobStatuses))
      .repeat(Schedule.spaced(restPollingInterval))
      .runDrain
      .forkDaemon

    jobOverviewPollFibers
      .get((namespace, name))
      .noneOrUnitZIO {
        procEffect.flatMap(fiber => jobOverviewPollFibers.put((namespace, name), fiber))
      }
  }

  private def unWatchJobOverviews(namespace: String, name: String): UIO[Unit] = {
    jobOverviewPollFibers
      .get((namespace, name))
      .someOrUnitZIO(fiber => fiber.interrupt)
  }

  /** Monitor Flink cluster metrics via cluster overview API and jm configuration API. */
  private def watchClusterMetrics(namespace: String, name: String): UIO[Unit] = {

    val effect = ZStream
      // retrieve rest endpoint
      .fromZIO(
        restSvcEndpointSnaps
          .get(namespace, name)
          .flatMap {
            case None           => ZIO.fail(RestEndpointNotFound)
            case Some(endpoint) =>
              // request cluster overview & jobmanager config api in parallel
              FlinkRestRequest(endpoint.chooseRest).getClusterOverview <&>
              FlinkRestRequest(endpoint.chooseRest).getJobmanagerConfig
          })
      .retry(Schedule.spaced(restRetryInterval))
      .map { case (clusterOv, jmConfigs) =>
        val totalJmMemory = FlinkMemorySizeParser
          .parse(jmConfigs.getOrElse("jobmanager.memory.process.size", "0b"))
          .map(_.mebiBytes)
          .map(e => Try(e.toInt).getOrElse(0))
          .getOrElse(0)

        val totalTmMemory = FlinkMemorySizeParser
          .parse(jmConfigs.getOrElse("taskmanager.memory.process.size", "0b"))
          .map(_.mebiBytes * clusterOv.taskManagers)
          .map(e => Try(e.toInt).getOrElse(0))
          .getOrElse(0)

        ClusterMetrics(
          totalJmMemory = totalJmMemory,
          totalTmMemory = totalTmMemory,
          totalTm = clusterOv.taskManagers,
          totalSlot = clusterOv.slotsTotal,
          availableSlot = clusterOv.slotsAvailable,
          runningJob = clusterOv.jobsRunning,
          cancelledJob = clusterOv.jobsFinished,
          failedJob = clusterOv.jobsFailed
        )
      }
      .tap(metrics => clusterMetricsSnaps.put((namespace, name), metrics))
      .repeat(Schedule.spaced(restPollingInterval))
      .runDrain
      .forkDaemon

    clusterMetricsPollFibers
      .get((namespace, name))
      .noneOrUnitZIO {
        effect.flatMap(fiber => clusterMetricsPollFibers.put((namespace, name), fiber)).unit
      }
  }

  // noinspection DuplicatedCode
  private def unWatchClusterMetrics(namespace: String, name: String): UIO[Unit] = {
    clusterMetricsPollFibers
      .get((namespace, name))
      .someOrUnitZIO(fiber => fiber.interrupt.unit)
  }

  private case object RestEndpointNotFound
}
