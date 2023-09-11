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

package org.apache.streampark.console.core.task

import org.apache.streampark.common.zio.ZIOContainerSubscription.{ConcurrentMapExtension, RefMapExtension}
import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.console.core.bean.AlertTemplate
import org.apache.streampark.console.core.entity.{Application, FlinkCluster}
import org.apache.streampark.console.core.enums.{FlinkAppState, OptionState}
import org.apache.streampark.console.core.service.FlinkClusterService
import org.apache.streampark.console.core.service.alert.AlertService
import org.apache.streampark.console.core.service.application.{ApplicationInfoService, ApplicationManageService}
import org.apache.streampark.console.core.utils.FlinkAppStateConverter
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.observer.{FlinkK8sObserver, Name, Namespace}
import org.apache.streampark.flink.kubernetes.v2.operator.OprError.TrackKeyNotFound

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import zio.{UIO, ZIO}
import zio.ZIO.logError
import zio.ZIOAspect.annotated

import java.util.Date

@Component
class FlinkK8sChangeListener {
  @Lazy @Autowired
  private var applicationManageService: ApplicationManageService = _

  @Lazy
  @Autowired
  private var applicationInfoService: ApplicationInfoService = _

  @Lazy @Autowired
  private var flinkClusterService: FlinkClusterService = _
  @Lazy @Autowired
  private var alertService: AlertService = _

  subscribeJobStatusChange.forkDaemon.runIO
  subscribeMetricsChange.forkDaemon.runIO
  subscribeRestSvcEndpointChange.forkDaemon.runIO

  private val alterStateList =
    Array(
      FlinkAppState.FAILED,
      FlinkAppState.LOST,
      FlinkAppState.RESTARTING,
      FlinkAppState.FINISHED)

  def subscribeJobStatusChange: UIO[Unit] = {
    FlinkK8sObserver.evaluatedJobSnaps
      .flatSubscribeValues()
      // Get Application records and convert JobSnapshot to Application
      .mapZIO {
        jobSnap =>
          ZIO
            .attemptBlocking {
              Option(applicationManageService.getById(jobSnap.appId))
                .map(app => setByJobStatusCV(app, jobSnap))
            }
            .catchAll {
              err =>
                logError(s"Fail to get Application records: ${err.getMessage}")
                  .as(None) @@ annotated("appId" -> jobSnap.appId.toString)
            }
      }
      .filter(_.nonEmpty)
      .map(_.get)
      // Save Application records
      .tap {
        app =>
          ZIO
            .attemptBlocking(applicationInfoService.persistMetrics(app))
            .retryN(3)
            .tapError(err => logError(s"Fail to persist Application status: ${err.getMessage}"))
            .ignore @@ annotated("appId" -> app.getAppId)
      }
      // Alert for unhealthy states in parallel
      .mapZIOPar(10) {
        app =>
          val state = FlinkAppState.of(app.getState)
          ZIO
            .attemptBlocking(alertService.alert(app.getAlertId(), AlertTemplate.of(app, state)))
            .when(alterStateList.contains(state))
            .retryN(3)
            .tapError(
              err => logError(s"Fail to alter unhealthy application state: ${err.getMessage}"))
            .ignore @@ annotated("appId" -> app.getAppId, "state" -> state.toString)
      }
      .runDrain
  }

  def subscribeMetricsChange: UIO[Unit] = {
    FlinkK8sObserver.clusterMetricsSnaps
      .flatSubscribe()
      .mapZIO {
        metricsSnap =>
          ZIO
            .attemptBlocking {
              val namespaceAndName: (Namespace, Name) = metricsSnap._1
              val trackKey: ZIO[Any, TrackKeyNotFound, TrackKey] = FlinkK8sObserver.trackedKeys
                .find(
                  trackedKey =>
                    trackedKey.clusterNamespace == namespaceAndName._1 && trackedKey.clusterName == namespaceAndName._2)
                .someOrFail(TrackKeyNotFound(namespaceAndName._1, namespaceAndName._2))

              Option(applicationManageService.getById(trackKey.map(_.id)), metricsSnap._2)
            }
            .catchAll {
              err =>
                logError(s"Fail to get Application records: ${err.getMessage}")
                  .as(None) @@ annotated("name" -> metricsSnap._1._2)
            }
      }
      .filter(_.nonEmpty)
      .map(_.get)
      .tap {
        appAndMetrics =>
          ZIO
            .attemptBlocking {
              val app: Application = appAndMetrics._1
              val clusterMetrics: ClusterMetrics = appAndMetrics._2
              app.setJmMemory(clusterMetrics.totalJmMemory)
              app.setTmMemory(clusterMetrics.totalTmMemory)
              app.setTotalTM(clusterMetrics.totalTm)
              app.setTotalSlot(clusterMetrics.totalSlot)
              app.setAvailableSlot(clusterMetrics.availableSlot)
              applicationInfoService.persistMetrics(app)
            }
            .retryN(3)
            .tapError(err => logError(s"Fail to persist Application Metrics: ${err.getMessage}"))
            .ignore @@ annotated("appId" -> appAndMetrics._1.getAppId)
      }
      .runDrain
  }

  def subscribeRestSvcEndpointChange: UIO[Unit] = {
    FlinkK8sObserver.restSvcEndpointSnaps
      .flatSubscribe()
      .foreach {
        restSvcEndpointSnap =>
          ZIO
            .attempt {

              val namespaceAndName: (Namespace, Name) = restSvcEndpointSnap._1
              val trackKey: ZIO[Any, TrackKeyNotFound, TrackKey] = FlinkK8sObserver.trackedKeys
                .find(
                  trackedKey =>
                    trackedKey.clusterNamespace == namespaceAndName._1 && trackedKey.clusterName == namespaceAndName._2)
                .someOrFail(TrackKeyNotFound(namespaceAndName._1, namespaceAndName._2))
              val restSvcEndpoint: RestSvcEndpoint = restSvcEndpointSnap._2

              val app: Application = applicationManageService.getById(trackKey.map(_.id))

              val flinkCluster: FlinkCluster = flinkClusterService.getById(app.getFlinkClusterId)

              if (restSvcEndpoint == null || restSvcEndpoint.ipRest == null) return ZIO.unit
              val url = restSvcEndpoint.ipRest
              app.setFlinkRestUrl(url)
              applicationInfoService.persistMetrics(app)

              flinkCluster.setAddress(url)
              flinkClusterService.update(flinkCluster)

            }
            .retryN(3)
            .ignore
      }
  }

  private def setByJobStatusCV(app: Application, jobSnapshot: JobSnapshot): Application = { // infer the final flink job state
    val state: FlinkAppState =
      FlinkAppStateConverter.k8sEvalJobStateToFlinkAppState(jobSnapshot.evalState)
    val jobStatusOption: Option[JobStatus] = jobSnapshot.jobStatus

    if (jobStatusOption.nonEmpty) {
      val jobStatus: JobStatus = jobStatusOption.get
      // corrective start-time / end-time / duration
      val preStartTime: Long =
        if (app.getStartTime != null) app.getStartTime.getTime
        else 0

      val startTime: Long = Math.max(jobStatus.startTs, preStartTime)
      val preEndTime: Long =
        if (app.getEndTime != null) app.getEndTime.getTime
        else 0
      var endTime: Long = Math.max(jobStatus.endTs.getOrElse(-1L), preEndTime)
      var duration: Long = if (app.getDuration != null) app.getDuration else 0
      if (FlinkAppState.isEndState(state.getValue)) {
        if (endTime < startTime) endTime = System.currentTimeMillis
        if (duration <= 0) duration = endTime - startTime
      }
      app.setJobId(jobStatus.jobId)
      val totalTask = if (jobStatus.tasks.nonEmpty) jobStatus.tasks.get.total else 0
      app.setTotalTask(totalTask)
      app.setStartTime(
        new Date(
          if (startTime > 0) startTime
          else 0))
      app.setEndTime(
        if (endTime > 0 && endTime >= startTime) new Date(endTime)
        else null)
      app.setDuration(
        if (duration > 0) duration
        else 0)
    }

    app.setState(state.getValue)
    // when a flink job status change event can be received, it means
    // that the operation command sent by streampark has been completed.
    app.setOptionState(OptionState.NONE.getValue)
    app
  }
}
