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

import org.apache.streampark.common.conf.K8sFlinkConfig
import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.zio.ZIOContainerSubscription.{ConcurrentMapExtension, RefMapExtension}
import org.apache.streampark.common.zio.ZIOExt.{IOOps, ZStreamOptionEffectOps}
import org.apache.streampark.console.core.bean.AlertTemplate
import org.apache.streampark.console.core.entity.Application
import org.apache.streampark.console.core.enums.{FlinkAppState, OptionState}
import org.apache.streampark.console.core.service.FlinkClusterService
import org.apache.streampark.console.core.service.alert.AlertService
import org.apache.streampark.console.core.service.application.ApplicationInfoService
import org.apache.streampark.console.core.utils.FlinkAppStateConverter
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey.ApplicationJobKey
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserverSnapSubscriptionHelper.{ClusterMetricsSnapsSubscriptionOps, RestSvcEndpointSnapsSubscriptionOps}

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import zio.{UIO, ZIO}
import zio.ZIO.{logError => zlogError}
import zio.ZIOAspect.annotated
import zio.stream.UStream

import javax.annotation.PostConstruct

import java.util.Date

/** Flink status change listener on Kubernetes. */
@Component
class FlinkK8sChangeListenerV2 extends Logger {

  @Lazy @Autowired
  private var applicationInfoService: ApplicationInfoService = _

  @Lazy @Autowired
  private var flinkClusterService: FlinkClusterService = _

  @Lazy @Autowired
  private var alertService: AlertService = _

  private val alterStateList =
    Array(
      FlinkAppState.FAILED,
      FlinkAppState.LOST,
      FlinkAppState.RESTARTING,
      FlinkAppState.FINISHED)

  @PostConstruct
  def init(): Unit = {
    // launch all subscription effect when enable flink-k8s v2
    lazy val launch = for {
      _ <- subscribeJobStatusChange.forkDaemon
      _ <- subscribeApplicationJobMetricsChange.forkDaemon
      _ <- subscribeApplicationJobRestSvcEndpointChange.forkDaemon
    } yield ()

    if (K8sFlinkConfig.isV2Enabled) {
      logInfo(
        s"Launch FlinkK8sChangeListenerV2 as enabled by the \"${K8sFlinkConfig.ENABLE_V2.key}\"")
      launch.runIO
    } else {
      logInfo(
        s"Skip launching FlinkK8sChangeListenerV2 as disabled by the \"${K8sFlinkConfig.ENABLE_V2.key}\"")
    }
  }

  /** Subscribe Flink job status change from FlinkK8sObserver */
  private def subscribeJobStatusChange: UIO[Unit] = {

    def process(subStream: UStream[JobSnapshot]): UStream[Unit] = subStream
      // Convert EvalJobState to FlinkAppState
      .map(snap => snap -> FlinkAppStateConverter.k8sEvalJobStateToFlinkAppState(snap.evalState))
      // Update the corresponding columns of Application record
      .tap {
        case (snap: JobSnapshot, convertedState: FlinkAppState) =>
          safeUpdateApplicationRecord(snap.appId) {
            wrapper =>
              // update JobStatus related columns
              snap.jobStatus match {
                case Some(status) =>
                  wrapper
                    .typedSet(_.getJobId, status.jobId)
                    .typedSet(_.getStartTime, new Date(status.startTs))
                    .typedSet(_.getEndTime, status.endTs.map(new Date(_)).orNull)
                    .typedSet(_.getDuration, status.duration)
                    .typedSet(_.getTotalTask, status.tasks.map(_.total).getOrElse(0))
                case None =>
                  wrapper
                    .typedSet(_.getJobId, null)
                    .typedSet(_.getStartTime, null)
                    .typedSet(_.getEndTime, null)
                    .typedSet(_.getDuration, null)
                    .typedSet(_.getTotalTask, null)
              }
              // Copy the logic from resources/mapper/core/ApplicationMapper.xml:persistMetrics
              if (FlinkAppState.isEndState(convertedState.getValue)) {
                wrapper
                  .typedSet(_.getTotalTM, null)
                  .typedSet(_.getTotalSlot, null)
                  .typedSet(_.getTotalSlot, null)
                  .typedSet(_.getAvailableSlot, null)
                  .typedSet(_.getJmMemory, null)
                  .typedSet(_.getTmMemory, null)
              }
              // Update job state column
              wrapper.typedSet(_.getState, convertedState.getValue)
              // when a flink job status change event can be received,
              // it means that the operation command sent by streampark has been completed.
              wrapper.typedSet(_.getOptions, OptionState.NONE.getValue)
          }
      }
      // Alert for unhealthy job states in parallel
      .filter { case (_, state) => alterStateList.contains(state) }
      .mapZIOPar(10) {
        case (snap, state) =>
          safeGetApplicationRecord(snap.appId).flatMap {
            case None => ZIO.unit
            case Some(app) =>
              ZIO
                .attemptBlocking(alertService.alert(app.getAlertId, AlertTemplate.of(app, state)))
                .retryN(3)
                .tapError(err => zlogError(s"Fail to alter unhealthy state: ${err.getMessage}"))
                .ignore @@
                annotated("appId" -> app.getId.toString, "state" -> state.toString)
          }
      }

    FlinkK8sObserver.evaluatedJobSnaps
      .flatSubscribeValues()
      // Handle events grouped by appId in parallel while each appId group would be executed in serial.
      .groupByKey(_.appId) { case (_, substream) => process(substream) }
      .runDrain
  }

  /** Subscribe Flink application job metrics change from FlinkK8sObserver */
  private def subscribeApplicationJobMetricsChange: UIO[Unit] = {
    FlinkK8sObserver.clusterMetricsSnaps
      .flatSubscribe()
      // Combine with the corresponding ApplicationJobKey
      .combineWithTypedTrackKey[ApplicationJobKey]
      .filterSome
      .groupByKey(_._1.id) {
        case (_, substream) =>
          // Update metrics info of the corresponding Application record
          substream.mapZIO {
            case (trackKey: ApplicationJobKey, metrics: ClusterMetrics) =>
              safeUpdateApplicationRecord(trackKey.id) {
                wrapper =>
                  wrapper
                    .typedSet(_.getJmMemory, metrics.totalJmMemory)
                    .typedSet(_.getTmMemory, metrics.totalTmMemory)
                    .typedSet(_.getTotalTM, metrics.totalTm)
                    .typedSet(_.getTotalSlot, metrics.totalSlot)
                    .typedSet(_.getAvailableSlot, metrics.availableSlot)
              }
          }
      }
      .runDrain
  }

  /** Subscribe K8s rest endpoint of Flink Application mode job from FlinkK8sObserver. */
  private def subscribeApplicationJobRestSvcEndpointChange: UIO[Unit] = {
    FlinkK8sObserver.restSvcEndpointSnaps
      .flatSubscribe()
      // Combine with the corresponding ApplicationJobKey
      .combineWithTypedTrackKey[ApplicationJobKey]
      .filterSome
      .groupByKey(_._1.id) {
        case (_, substream) =>
          // Update jobManagerUrl of the corresponding Application record
          substream.mapZIO {
            case (key: ApplicationJobKey, endpoint: RestSvcEndpoint) =>
              safeUpdateApplicationRecord(key.id)(
                wrapper => wrapper.typedSet(_.getJobManagerUrl, endpoint.ipRest))
          }
      }
      .runDrain
  }

  // Get Application record by appId from persistent storage.
  private def safeGetApplicationRecord(appId: Long): UIO[Option[Application]] = {
    ZIO
      .attemptBlocking(Option(applicationInfoService.getById(appId)))
      .catchAll(err => zlogError(s"Fail to get Application record: ${err.getMessage}").as(None))
  } @@ annotated("appId" -> appId.toString)

  // Update Application record by appId into persistent storage.
  private def safeUpdateApplicationRecord(appId: Long)(
      mapperSetFunc: LambdaUpdateWrapper[Application] => Unit): UIO[Unit] = {
    ZIO
      .attemptBlocking {
        val wrapper = new LambdaUpdateWrapper[Application]()
        mapperSetFunc(wrapper)
        wrapper.eq((e: Application) => e.getId, appId)
        applicationInfoService.update(null, wrapper)
      }
      .retryN(3)
      .tapError(err => zlogError(s"Fail to update Application record: ${err.getMessage}"))
      .ignore
  } @@ annotated("appId" -> appId.toString)

  implicit private class ApplicationLambdaUpdateOps(wrapper: LambdaUpdateWrapper[Application]) {
    def typedSet[Value](
        func: Application => Value,
        value: Value): LambdaUpdateWrapper[Application] = {
      wrapper.set((e: Application) => func(e), value); wrapper
    }
  }

}
