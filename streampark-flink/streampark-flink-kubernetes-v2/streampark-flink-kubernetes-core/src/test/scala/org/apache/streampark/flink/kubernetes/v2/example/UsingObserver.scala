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

package org.apache.streampark.flink.kubernetes.v2.example

import org.apache.streampark.common.zio.PrettyStringOps
import org.apache.streampark.common.zio.ZIOContainerSubscription.{ConcurrentMapExtension, RefMapExtension}
import org.apache.streampark.common.zio.ZIOExt.{unsafeRun, ZStreamOps}
import org.apache.streampark.flink.kubernetes.v2.model.TrackKey
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver

import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import zio.{durationInt, Console, ZIO}

class UsingObserver extends AnyWordSpecLike with BeforeAndAfterAll {

  "Track and get flink job snapshot." in unsafeRun {
    for {
      // track resource
      _       <- ZIO.unit
      trackId  = TrackKey.appJob(233, "fdev", "simple-appjob")
      _       <- FlinkK8sObserver.track(trackId)
      // get job snapshot
      _       <- ZIO.sleep(3.seconds)
      jobSnap <- FlinkK8sObserver.evaluatedJobSnaps.getValue(trackId.id)
      _       <- Console.printLine(jobSnap.prettyStr)
    } yield ()
  }

  "Track and get flink cluster metrics" in unsafeRun {
    for {
      // track resource
      _       <- ZIO.unit
      trackId  = TrackKey.appJob(233, "fdev", "simple-appjob")
      _       <- FlinkK8sObserver.track(trackId)
      // get job snapshot
      _       <- ZIO.sleep(3.seconds)
      jobSnap <- FlinkK8sObserver.clusterMetricsSnaps.get((trackId.namespace, trackId.name))
      _       <- Console.printLine(jobSnap.prettyStr)
    } yield ()
  }

  "Subscribe Flink job snapshots changes." in unsafeRun {
    for {
      // track resource
      _          <- FlinkK8sObserver.track(TrackKey.sessionJob(233, "fdev", "simple-sessionjob", "simple-session"))
      _          <- FlinkK8sObserver.track(TrackKey.appJob(234, "fdev", "simple-appjob"))
      // subscribe job status changes
      watchStream = FlinkK8sObserver.evaluatedJobSnaps.flatSubscribe()
      _          <- watchStream.debugPretty.runDrain
    } yield ()
  }

  "Only subscribe Flink job state changes." in unsafeRun {
    for {
      _ <- FlinkK8sObserver.track(TrackKey.appJob(234, "fdev", "simple-appjob"))
      _ <- FlinkK8sObserver.evaluatedJobSnaps
             .flatSubscribe()
             .map { case (appId, status) => (appId, status.evalState) }
             .diffPrev
             .debug
             .runDrain
    } yield ()
  }

  "Subscribe Flink cluster metrics changes." in unsafeRun {
    for {
      // track resource
      _          <- FlinkK8sObserver.track(TrackKey.sessionJob(233, "fdev", "simple-sessionjob", "simple-session"))
      _          <- FlinkK8sObserver.track(TrackKey.appJob(234, "fdev", "simple-appjob"))
      // subscribe job status changes
      watchStream = FlinkK8sObserver.clusterMetricsSnaps.flatSubscribe()
      _          <- watchStream.debugPretty.runDrain
    } yield ()
  }

}
