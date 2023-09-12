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
import org.apache.streampark.common.enums.{ClusterState, ExecutionMode}
import org.apache.streampark.common.zio.{OptionTraversableOps, PrettyStringOps}
import org.apache.streampark.common.zio.ZIOContainerSubscription.{ConcurrentMapExtension, RefMapExtension}
import org.apache.streampark.common.zio.ZIOExt.{IterableZStreamConverter, OptionZIOOps, UIOOps, ZStreamOptionEffectOps}
import org.apache.streampark.console.core.bean.AlertTemplate
import org.apache.streampark.console.core.entity.{Application, FlinkCluster}
import org.apache.streampark.console.core.enums.{FlinkAppState, OptionState}
import org.apache.streampark.console.core.service.FlinkClusterService
import org.apache.streampark.console.core.service.alert.AlertService
import org.apache.streampark.console.core.service.application.ApplicationInfoService
import org.apache.streampark.console.core.utils.FlinkK8sDataTypeConverter
import org.apache.streampark.console.core.utils.FlinkK8sDataTypeConverter.{clusterMetricsToFlinkMetricCV, flinkClusterToClusterKey, k8sDeployStateToClusterState}
import org.apache.streampark.console.core.utils.MybatisScalaExt.{lambdaQuery, lambdaUpdate, LambdaQueryOps, LambdaUpdateOps}
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey.{ApplicationJobKey, ClusterKey}
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserverSnapSubscriptionHelper.{ClusterMetricsSnapsSubscriptionOps, DeployCRSnapsSubscriptionOps, RestSvcEndpointSnapsSubscriptionOps}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import zio.{Fiber, Ref, UIO, ZIO}
import zio.ZIO.{interruptible, logInfo}
import zio.ZIOAspect.annotated
import zio.stream.UStream

import javax.annotation.{PostConstruct, PreDestroy}

import java.lang
import java.util.Date

import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

/** Broker of FlinkK8sObserver which is the observer for Flink on Kubernetes */
@Component
class FlinkK8sObserverBroker @Autowired() (
    var applicationInfoService: ApplicationInfoService,
    var flinkClusterService: FlinkClusterService,
    var alertService: AlertService)
  extends FlinkK8sObserverStub
  with FlinkK8sObserverBrokerSidecar { it =>

  private val observer = FlinkK8sObserver

  private val alertJobStateList: Array[FlinkAppState] = Array(
    FlinkAppState.FAILED,
    FlinkAppState.LOST,
    FlinkAppState.RESTARTING,
    FlinkAppState.FINISHED
  )

  private val alertClusterStateList = Array(
    ClusterState.FAILED,
    ClusterState.UNKNOWN,
    ClusterState.LOST,
    ClusterState.KILLED
  )

  private lazy val allDaemonEffects: Array[UIO[Unit]] = Array(
    subscribeJobStatusChange,
    subscribeApplicationJobMetricsChange,
    subscribeApplicationJobRestSvcEndpointChange,
    subscribeGlobalClusterMetricChange,
    subscribeClusterRestSvcEndpointChange,
    subscribeClusterStateChange
  )

  // All fibers running on the daemon。
  private val daemonFibers   = Ref.make(Array.empty[Fiber.Runtime[Nothing, Unit]]).runUIO
  // Aggregated flink cluster metrics by teamId
  private val aggFlinkMetric = Ref.make(Map.empty[Long, ClusterMetrics]).runUIO

  @PostConstruct
  def init(): Unit = {
    val effect: UIO[Unit] = for {
      // launch all subscription fibers
      fibers       <- ZIO.foreach(allDaemonEffects)(interruptible(_).forkDaemon)
      // async restore track keys from persistent storage
      restoreFiber <- restoreTrackKeyRecords.forkDaemon
      _            <- daemonFibers.set(fibers :+ restoreFiber)
      _            <- logInfo("Launch FlinkK8sChangeListenerV2.")
    } yield ()
    effect.when(K8sFlinkConfig.isV2Enabled).runUIO
  }

  @PreDestroy
  def destroy(): Unit = {
    daemonFibers.get.flatMap(fibers => ZIO.foreach(fibers)(_.interrupt)).runUIO
  }

  /** Restore track list from persistent storage into FlinkK8sObserver. */
  private def restoreTrackKeyRecords: UIO[Unit] = {
    import ExecutionMode._

    def convertAppToTrackKey(app: Application): Option[TrackKey] = app.getExecutionModeEnum match {
      case KUBERNETES_NATIVE_APPLICATION => Some(TrackKey.appJob(app.getId, app.getK8sNamespace, app.getClusterId))
      case KUBERNETES_NATIVE_SESSION     =>
        Option(app.getK8sName) match {
          case Some(name) => Some(TrackKey.sessionJob(app.getId, app.getK8sNamespace, name, app.getClusterId))
          case None       =>
            Some(TrackKey.unmanagedSessionJob(app.getId, app.getK8sNamespace, app.getClusterId, app.getJobId))
        }

      case _ => None
    }

    val fromApplicationRecords: UIO[Unit] = it
      .safeFindApplication(
        lambdaQuery[Application].typedIn(_.getExecutionMode, ExecutionMode.getKubernetesMode.asScala)
      )(10)
      .map(apps => apps.map(app => convertAppToTrackKey(app)).filterSome.toVector)
      .tap(keys => logInfo(s"Restore Flink K8s track-keys from Application records:\n${keys.prettyStr}"))
      .flatMap(keys => ZIO.foreachDiscard(keys)(observer.track))

    val fromFlinkClusterRecords: UIO[Unit] = it
      .safeFindFlinkClusterRecord(
        lambdaQuery[FlinkCluster].typedEq(_.getExecutionMode, KUBERNETES_NATIVE_SESSION.getMode)
      )(10)
      .map(clusters => clusters.map(e => TrackKey.cluster(e.getId, e.getK8sNamespace, e.getClusterId)))
      .tap(keys => logInfo(s"Restore Flink K8s track-keys from FlinkCluster records:\n${keys.prettyStr}"))
      .flatMap(keys => ZIO.foreachDiscard(keys)(observer.track))

    fromApplicationRecords <&> fromFlinkClusterRecords
  }

  /** Subscribe Flink job status change from FlinkK8sObserver */
  private def subscribeJobStatusChange: UIO[Unit] = {

    def process(subStream: UStream[JobSnapshot]): UStream[Unit] = subStream
      // Convert EvalJobState to FlinkAppState
      .map(snap => snap -> FlinkK8sDataTypeConverter.k8sEvalJobStateToFlinkAppState(snap.evalState))
      // Update the corresponding columns of Application record
      .tap { case (snap: JobSnapshot, convertedState: FlinkAppState) =>
        safeUpdateApplicationRecord(snap.appId) {

          var update = lambdaUpdate[Application]
            .typedSet(_.getState, convertedState.getValue)
            .typedSet(_.getOptions, OptionState.NONE.getValue)
          // update JobStatus related columns
          snap.jobStatus.foreach { status =>
            update = update
              .typedSet(_.getJobId, status.jobId)
              .typedSet(_.getStartTime, new Date(status.startTs))
              .typedSet(_.getEndTime, status.endTs.map(new Date(_)).orNull)
              .typedSet(_.getDuration, status.duration)
              .typedSet(_.getTotalTask, status.tasks.map(_.total).getOrElse(0))
          }
          // Copy the logic from resources/mapper/core/ApplicationMapper.xml:persistMetrics
          if (FlinkAppState.isEndState(convertedState.getValue)) {
            update = update
              .typedSet(_.getTotalTM, null)
              .typedSet(_.getTotalSlot, null)
              .typedSet(_.getTotalSlot, null)
              .typedSet(_.getAvailableSlot, null)
              .typedSet(_.getJmMemory, null)
              .typedSet(_.getTmMemory, null)
          }
          update
        }
      }
      // Alert for unhealthy job states in parallel
      .filter { case (_, state) => alertJobStateList.contains(state) }
      .mapZIOPar(5) { case (snap, state) =>
        safeGetApplicationRecord(snap.appId).someOrUnitZIO { app =>
          ZIO
            .attemptBlocking(alertService.alert(app.getAlertId, AlertTemplate.of(app, state)))
            .retryN(3)
            .tapError(err => logInfo(s"Fail to alter unhealthy state: ${err.getMessage}"))
            .ignore @@
          annotated("appId" -> app.getId.toString, "state" -> state.toString)
        }
      }

    observer.evaluatedJobSnaps
      .flatSubscribeValues()
      // Handle events grouped by appId in parallel while each appId group would be executed in serial.
      .groupByKey(_.appId) { case (_, substream) => process(substream) }
      .runDrain
  }

  /** Subscribe Flink application job metrics change from FlinkK8sObserver */
  private def subscribeApplicationJobMetricsChange: UIO[Unit] = {
    observer.clusterMetricsSnaps
      .flatSubscribe()
      // Combine with the corresponding ApplicationJobKey
      .combineWithTypedTrackKey[ApplicationJobKey]
      .filterSome
      .groupByKey(_._1.id) { case (_, substream) =>
        // Update metrics info of the corresponding Application record
        substream.mapZIO { case (trackKey: ApplicationJobKey, metrics: ClusterMetrics) =>
          safeUpdateApplicationRecord(trackKey.id)(
            lambdaUpdate[Application]
              .typedSet(_.getJmMemory, metrics.totalJmMemory)
              .typedSet(_.getTmMemory, metrics.totalTmMemory)
              .typedSet(_.getTotalTM, metrics.totalTm)
              .typedSet(_.getTotalSlot, metrics.totalSlot)
              .typedSet(_.getAvailableSlot, metrics.availableSlot))
        }
      }
      .runDrain
  }

  /** Subscribe Flink cluster status change from FlinkK8sObserver */
  private def subscribeClusterStateChange: UIO[Unit] = {

    def process(substream: UStream[(ClusterKey, (DeployCRStatus, Option[JobStatus]))]): UStream[Unit] = {
      substream
        // Convert K8s CR status to ClusterState
        .map { case (key, (crStatus, _)) =>
          (key.id, k8sDeployStateToClusterState(crStatus), crStatus.error)
        }
        // Update the corresponding FlinkCluster record
        .tap { case (id, state, error) =>
          safeUpdateFlinkClusterRecord(id)(
            lambdaUpdate[FlinkCluster]
              .typedSet(_.getClusterState, state.getValue)
              .typedSet(error.isDefined, _.getException, error.get))
        }
        // Alter for unhealthy state in parallel
        .filter { case (_, state, _) => alertClusterStateList.contains(state) }
        .mapZIOPar(5) { case (id, state, _) =>
          safeGetFlinkClusterRecord(id).someOrUnitZIO { flinkCluster =>
            ZIO
              .attemptBlocking(alertService.alert(flinkCluster.getAlertId, AlertTemplate.of(flinkCluster, state)))
              .retryN(5)
              .tapError(err => logInfo(s"Fail to alter unhealthy state: ${err.getMessage}"))
              .ignore @@
            annotated("FlinkCluster.id" -> id.toString, "state" -> state.toString)
          }
        }
    }

    observer.deployCRSnaps
      .flatSubscribe()
      // Combine with the corresponding ClusterKey
      .combineWithTypedTrackKey[ClusterKey]
      .filterSome
      // Handle events grouped by id in parallel while each group would be executed in serial.
      .groupByKey(_._1) { case (_, substream) => process(substream) }
      .runDrain
  }

  /** Subscribe K8s rest endpoint of Flink Application mode job from FlinkK8sObserver. */
  private def subscribeApplicationJobRestSvcEndpointChange: UIO[Unit] = {
    observer.restSvcEndpointSnaps
      .flatSubscribe()
      // Combine with the corresponding ApplicationJobKey
      .combineWithTypedTrackKey[ApplicationJobKey]
      .filterSome
      .groupByKey(_._1.id) { case (_, substream) =>
        // Update jobManagerUrl of the corresponding Application record
        substream.mapZIO { case (key: ApplicationJobKey, endpoint: RestSvcEndpoint) =>
          safeUpdateApplicationRecord(key.id)(lambdaUpdate[Application].typedSet(_.getJobManagerUrl, endpoint.ipRest))
        }
      }
      .runDrain
  }

  /** Subscribe K8s rest endpoint of Flink cluster from FlinkK8sObserver. */
  private def subscribeClusterRestSvcEndpointChange: UIO[Unit] = {
    observer.restSvcEndpointSnaps
      .flatSubscribe()
      // Combine with the corresponding ClusterKey
      .combineWithTypedTrackKey[ClusterKey]
      .filterSome
      .groupByKey(_._1) { case (_, substream) =>
        substream.mapZIO { case (key: ClusterKey, endpoint: RestSvcEndpoint) =>
          safeUpdateFlinkClusterRecord(key.id)(
            lambdaUpdate[FlinkCluster]
              .typedSet(_.getAddress, endpoint.ipRest)
              .typedSet(_.getJobManagerUrl, endpoint.ipRest))
        }
      }
      .runDrain
  }

  /** Subscribe Flink cluster metrics change from FlinkK8sObserver and aggregate it by teamId */
  private def subscribeGlobalClusterMetricChange: UIO[Unit] = {
    observer.clusterMetricsSnaps
      .subscribe()
      .mapZIO { metricItems =>
        for {
          // Combine with appIds
          trackKeys         <- FlinkK8sObserver.trackedKeys.toSet
          metricWithAppIds   = metricItems.map { case ((ns, name), metric) =>
                                 metric ->
                                 trackKeys.filter(k => k.clusterNamespace == ns && k.clusterName == name).map(_.id)
                               }
          // Combine with teamId from persistent application records in parallel
          itemIdWithMetrics <- metricWithAppIds.asZStream
                                 .flatMapPar(10) { case (metric, appIds) =>
                                   appIds.asZStream
                                     .mapZIOParUnordered(10)(appId => safeGetApplicationRecord(appId))
                                     .map(_.flatMap(app => Option(app.getTeamId)))
                                     .filterSome
                                     .map(teamId => teamId.toLong -> metric)
                                 }
                                 .runCollect
          // Groups ClusterMetrics by teamId and aggregate for each grouping
          aggMetricsMap      = itemIdWithMetrics
                                 .groupBy { case (itemId, _) => itemId }
                                 .map { case (itemId, metric) =>
                                   itemId -> metric.map(_._2).foldLeft(ClusterMetrics.empty)((acc, e) => acc + e)
                                 }
          // Update aggFlinkMetric cache
          _                 <- aggFlinkMetric.set(aggMetricsMap)
        } yield ()
      }
      .runDrain
  }

  /** Stub method: Get aggregated metrics of all flink jobs on k8s cluster by team-id */
  override def getAggClusterMetric(teamId: lang.Long): ClusterMetrics = {
    for {
      metrics <- aggFlinkMetric.get
      result   = metrics.get(teamId)
    } yield result.getOrElse(ClusterMetrics.empty)
  }.runUIO

  override def getAggClusterMetricCV(teamId: lang.Long): FlinkMetricCV = {
    clusterMetricsToFlinkMetricCV(getAggClusterMetric(teamId))
  }

  /** Stub method: Add FlinkCluster to the watchlist. */
  override def watchFlinkCluster(flinkCluster: FlinkCluster): Unit = {
    ZIO
      .succeed(flinkClusterToClusterKey(flinkCluster))
      .someOrUnitZIO { trackKey =>
        observer.track(trackKey) *>
        logInfo("Add FlinkCluster into k8s observer tracking list") @@
        annotated("id" -> flinkCluster.getId.toString)
      }
      .runUIO
  }

  /**
   * Stub method: Remove FlinkCluster from the watchlist.
   * When there are associated SessionJobs with FlinkCluster,
   * the tracking of FlinkCluster will not be removed in reality.
   */
  override def unwatchFlinkCluster(flinkCluster: FlinkCluster): Unit = {
    ZIO
      .succeed(flinkClusterToClusterKey(flinkCluster))
      .someOrUnitZIO(trackKey => FlinkK8sObserver.untrack(trackKey))
      .runUIO
  }

  /** Stub method: Notify FlinkK8sObserver to remove tracking resources by TrackKey.id. */
  override def unWatchById(id: lang.Long): Unit = {
    observer.trackedKeys
      .find(_.id == id)
      .someOrUnitZIO(key => observer.untrack(key))
  }

}
