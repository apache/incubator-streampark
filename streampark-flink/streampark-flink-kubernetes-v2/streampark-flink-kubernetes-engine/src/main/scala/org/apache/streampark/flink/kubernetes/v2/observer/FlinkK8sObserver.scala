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

import org.apache.streampark.common.zio.ZIOExt.UIOOps
import org.apache.streampark.flink.kubernetes.v2.K8sTools.usingK8sClient
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey._

import org.apache.flink.v1beta1.{FlinkDeployment, FlinkDeploymentSpec, FlinkSessionJob, FlinkSessionJobSpec}
import zio.{IO, Ref, Schedule, UIO}
import zio.ZIO.logInfo
import zio.concurrent.{ConcurrentMap, ConcurrentSet}
import zio.stream.ZStream

/** Flink Kubernetes resource observer. */
sealed trait FlinkK8sObserver {

  /** Start tracking resources. */
  def track(key: TrackKey): UIO[Unit]

  /** Stop tracking resources. */
  def untrack(key: TrackKey): UIO[Unit]

  /** All tracked key in observer. */
  def trackedKeys: ConcurrentSet[TrackKey]

  /**
   * Snapshots of the Flink jobs that have been evaluated.
   *
   * This value can be subscribed as a ZStream structure.
   * Example:
   * {{{
   *   val stream: UStream[Chunk[(AppId, JobSnapshot)]] = evaluatedJobSnaps.subscribe()
   *   val stream: UStream[(AppId, JobSnapshot)] = evaluatedJobSnaps.flatSubscribe()
   *   val stream: UStream[JobSnapshot] = evaluatedJobSnaps.flatSubscribeValues()
   * }}}
   * Ref: [[org.apache.streampark.common.zio.ZIOContainerSubscription.RefMapExtension]]
   */
  def evaluatedJobSnaps: Ref[Map[AppId, JobSnapshot]]

  /**
   * Flink rest service endpoint snapshots cache.
   *
   * This value can be subscribed as a ZStream structure.
   * {{{
   *   val stream: UStream[Chunk[((Namespace, Name), RestSvcEndpoint)]] = restSvcEndpointSnaps.subscribe()
   *   val stream: UStream[((Namespace, Name), RestSvcEndpoint)] = restSvcEndpointSnaps.flatSubscribe()
   *   val stream: UStream[RestSvcEndpoint] = restSvcEndpointSnaps.flatSubscribeValues()
   * }}}
   * Ref: [[org.apache.streampark.common.zio.ZIOContainerSubscription.ConcurrentMapExtension]]
   */
  def restSvcEndpointSnaps: ConcurrentMap[(Namespace, Name), RestSvcEndpoint]

  /**
   * Flink cluster metrics snapshots.
   *
   * This value can be subscribed as a ZStream structure.
   * {{{
   *   val stream: UStream[Chunk[((Namespace, Name), ClusterMetrics)]] = clusterMetricsSnaps.subscribe()
   *   val stream: UStream[((Namespace, Name), ClusterMetrics)] = clusterMetricsSnaps.flatSubscribe()
   *   val stream: UStream[ClusterMetrics] = clusterMetricsSnaps.flatSubscribeValues()
   * }}}
   * Ref: [[org.apache.streampark.common.zio.ZIOContainerSubscription.ConcurrentMapExtension]]
   */
  def clusterMetricsSnaps: ConcurrentMap[(Namespace, Name), ClusterMetrics]

  /** Get Flink Deployment CR spec from K8s. */
  def getFlinkDeploymentCrSpec(ns: String, name: String): IO[Throwable, Option[FlinkDeploymentSpec]]

  /** Get Flink SessionJob CR spec from K8s. */
  def getFlinkSessionJobCrSpec(ns: String, name: String): IO[Throwable, Option[FlinkSessionJobSpec]]

}

object FlinkK8sObserver extends FlinkK8sObserver {

  // The following is a visible external snapshot.
  val trackedKeys          = ConcurrentSet.empty[TrackKey].runUIO
  val evaluatedJobSnaps    = Ref.make(Map.empty[AppId, JobSnapshot]).runUIO
  val restSvcEndpointSnaps = ConcurrentMap.empty[(Namespace, Name), RestSvcEndpoint].runUIO
  val clusterMetricsSnaps  = ConcurrentMap.empty[(Namespace, Name), ClusterMetrics].runUIO

  // In general, there is no need to view these snapshots externally.
  val deployCRSnaps         = ConcurrentMap.empty[(Namespace, Name), (DeployCRStatus, Option[JobStatus])].runUIO
  val sessionJobCRSnaps     = ConcurrentMap.empty[(Namespace, Name), (SessionJobCRStatus, Option[JobStatus])].runUIO
  val clusterJobStatusSnaps = ConcurrentMap.empty[(Namespace, Name), Vector[JobStatus]].runUIO

  private val restSvcEndpointObserver = RestSvcEndpointObserver(restSvcEndpointSnaps)
  private val deployCrObserver        = DeployCRObserver(deployCRSnaps)
  private val sessionJobCRObserver    = SessionJobCRObserver(sessionJobCRSnaps)
  private val clusterObserver         = RawClusterObserver(restSvcEndpointSnaps, clusterJobStatusSnaps, clusterMetricsSnaps)

  // Auto eval job snapshots forever.
  evalJobSnapshot
    .repeat(Schedule.spaced(evalJobSnapInterval))
    .forever
    .forkDaemon
    .runUIO

  /** Start tracking resources. */
  override def track(key: TrackKey): UIO[Unit] = {

    def trackCluster(ns: String, name: String): UIO[Unit] = {
      for {
        _ <- deployCrObserver.watch(ns, name)
        _ <- restSvcEndpointObserver.watch(ns, name)
        _ <- clusterObserver.watch(ns, name)
      } yield ()
    }

    def trackSessionJob(ns: String, name: String, refDeployName: String): UIO[Unit] = {
      sessionJobCRObserver.watch(ns, name) *>
      trackCluster(ns, refDeployName)
    }

    for {
      _ <- key match {
             case ApplicationJobKey(_, ns, name)                     => trackCluster(ns, name)
             case SessionJobKey(_, ns, name, clusterName)            => trackSessionJob(ns, name, clusterName)
             case UnmanagedSessionJobKey(_, clusterNs, clusterId, _) => trackCluster(clusterNs, clusterId)
             case ClusterKey(_, ns, name)                            => trackCluster(ns, name)
           }
      _ <- trackedKeys.add(key).unit
      _ <- logInfo(s"Start watching Flink resource: $key")
    } yield ()
  }

  /** Stop tracking resources. */
  override def untrack(key: TrackKey): UIO[Unit] = {

    def unTrackCluster(ns: String, name: String): UIO[Unit] = for {
      _ <- deployCrObserver.unWatch(ns, name)
      _ <- restSvcEndpointObserver.unWatch(ns, name)
      _ <- clusterObserver.unWatch(ns, name)
    } yield ()

    def unTrackSessionJob(ns: String, name: String) = {
      sessionJobCRObserver.unWatch(ns, name)
    }

    def unTrackPureCluster(ns: String, name: String) = unTrackCluster(ns, name).whenZIO {
      trackedKeys.toSet
        .map(set =>
          // When a flink cluster is referenced by another resource, tracking of that cluster is maintained.
          set.find {
            case k: ApplicationJobKey if k.namespace == ns && k.name == name                  => true
            case k: SessionJobKey if k.namespace == ns && k.clusterName == name               => true
            case k: UnmanagedSessionJobKey if k.clusterNamespace == ns && k.clusterId == name => true
            case _                                                                            => false
          })
        .map(_.isEmpty)
    }

    def unTrackUnmanagedSessionJob(clusterNs: String, clusterName: String) =
      unTrackCluster(clusterNs, clusterName).whenZIO {
        trackedKeys.toSet
          .map(set =>
            // When a flink cluster is referenced by another resource, tracking of that cluster is maintained.
            set.find {
              case k: ApplicationJobKey if k.namespace == clusterNs && k.name == clusterName    => true
              case k: SessionJobKey if k.namespace == clusterNs && k.clusterName == clusterName => true
              case k: ClusterKey if k.namespace == clusterNs && k.name == clusterName           => true
              case _                                                                            => false
            })
          .map(_.isEmpty)
      }.unit

    for {
      _ <- key match {
             case ApplicationJobKey(_, ns, name)                       => unTrackCluster(ns, name)
             case SessionJobKey(_, ns, name, _)                        => unTrackSessionJob(ns, name)
             case ClusterKey(_, ns, name)                              => unTrackPureCluster(ns, name)
             case UnmanagedSessionJobKey(_, clusterNs, clusterName, _) =>
               unTrackUnmanagedSessionJob(clusterNs, clusterName)
           }
      _ <- trackedKeys.remove(key)
      _ <- logInfo(s"Stop watching Flink resource: $key")
    } yield ()
  }

  /** Re-evaluate all job status snapshots from caches. */
  private def evalJobSnapshot: UIO[Unit] = {

    def mergeJobStatus(crStatus: Option[JobStatus], restStatus: Option[JobStatus]) =
      (crStatus, restStatus) match {
        case (Some(e), None)        => Some(e)
        case (None, Some(e))        => Some(e)
        case (None, None)           => None
        case (Some(cr), Some(rest)) =>
          Some(
            if (rest.updatedTs > cr.updatedTs) rest
            else cr.copy(endTs = rest.endTs, tasks = rest.tasks)
          )
      }

    ZStream
      .fromIterableZIO(trackedKeys.toSet)
      .filter { key =>
        key.isInstanceOf[ApplicationJobKey] || key.isInstanceOf[SessionJobKey] || key
          .isInstanceOf[UnmanagedSessionJobKey]
      }
      // Evaluate job snapshots for each TrackKey in parallel.
      .mapZIOParUnordered(evalJobSnapParallelism) {
        case ApplicationJobKey(id, ns, name) =>
          for {
            crSnap           <- deployCRSnaps.get(ns, name)
            restJobStatusVec <- clusterJobStatusSnaps.get((ns, name))
            crStatus          = crSnap.map(_._1)

            jobStatusFromCr   = crSnap.flatMap(_._2)
            jobStatusFromRest = restJobStatusVec.flatMap(_.headOption)
            finalJobStatus    = mergeJobStatus(jobStatusFromCr, jobStatusFromRest)

          } yield JobSnapshot.eval(id, ns, name, crStatus, finalJobStatus)

        case SessionJobKey(id, ns, name, clusterName) =>
          for {
            sessionJobSnap   <- sessionJobCRSnaps.get(ns, name)
            restJobStatusVec <- clusterJobStatusSnaps.get((ns, clusterName))
            crStatus          = sessionJobSnap.map(_._1)

            jobStatusFromCr   = sessionJobSnap.flatMap(_._2)
            jobId             = jobStatusFromCr.map(_.jobId).getOrElse("")
            jobStatusFromRest = restJobStatusVec.flatMap(_.find(_.jobId == jobId))
            finalJobStatus    = mergeJobStatus(jobStatusFromCr, jobStatusFromRest)

          } yield JobSnapshot.eval(id, ns, clusterName, crStatus, finalJobStatus)

        case UnmanagedSessionJobKey(id, clusterNs, clusterName, jid) =>
          for {
            restJobStatusVec <- clusterJobStatusSnaps.get((clusterNs, clusterName))
            jobStatus         = restJobStatusVec.flatMap(_.find(_.jobId == jid))
          } yield JobSnapshot.eval(id, clusterNs, clusterName, None, jobStatus)
      }
      // Collect result and Refresh evaluatedJobSnaps cache
      .runCollect
      .map(chunk => chunk.map(snap => (snap.appId, snap)).toMap)
      .flatMap(map => evaluatedJobSnaps.set(map))
      .unit
  }

  /** Get Flink Deployment CR spec from K8s. */
  override def getFlinkDeploymentCrSpec(ns: String, name: String): IO[Throwable, Option[FlinkDeploymentSpec]] =
    usingK8sClient { client =>
      Option(
        client
          .resources(classOf[FlinkDeployment])
          .inNamespace(ns)
          .withName(name)
          .get()
      ).map(_.getSpec)
    }

  /** Get Flink SessionJob CR spec from K8s. */
  override def getFlinkSessionJobCrSpec(ns: String, name: String): IO[Throwable, Option[FlinkSessionJobSpec]] = {
    usingK8sClient { client =>
      Option(
        client
          .resources(classOf[FlinkSessionJob])
          .inNamespace(ns)
          .withName(name)
          .get()
      ).map(_.getSpec)
    }
  }

}
