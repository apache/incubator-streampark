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

import org.apache.streampark.common.zio.{liftValueAsSome, PrettyStringOps}
import org.apache.streampark.common.zio.ZIOContainerSubscription.{ConcurrentMapExtension, RefMapExtension}
import org.apache.streampark.common.zio.ZIOExt.{unsafeRun, IOOps, ZStreamOps}
import org.apache.streampark.flink.kubernetes.v2.fs.EmbeddedFileServer
import org.apache.streampark.flink.kubernetes.v2.model._
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator

import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion
import org.scalatest.{BeforeAndAfterAll, Ignore}
import org.scalatest.wordspec.AnyWordSpecLike
import zio.{Console, ZIO}

/**
 * Example of using FlinkK8sOperator.
 * Tips: Please uncomment the @Ignore tag to execute the example code.
 *
 * Prerequisites for running this example：
 *  1. There exists a locally connectable Kubernetes cluster.
 *  2. Flink Kubernetes operator has been installed on Kubernetes.
 *     see: https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/docs/try-flink-kubernetes-operator/quick-start/
 *  3. A namespace named "fdev" has been created in Kubernetes.
 */
// @Ignore
class UsingOperator extends AnyWordSpecLike with BeforeAndAfterAll {

  "Deploy a simple Flink application job to Kubernetes" in unsafeRun {

    val spec = FlinkDeploymentDef(
      name = "simple-appjob",
      namespace = "fdev",
      image = "flink:1.16",
      flinkVersion = FlinkVersion.V1_16,
      jobManager = JobManagerDef(cpu = 1, memory = "1024m"),
      taskManager = TaskManagerDef(cpu = 1, memory = "1024m"),
      job = JobDef(
        jarURI = "local:///opt/flink/examples/streaming/StateMachineExample.jar",
        parallelism = 1
      )
    )
    for {
      _ <- FlinkK8sOperator.deployApplicationJob(114514, spec)
      // subscribe job status
      _ <- FlinkK8sObserver.evaluatedJobSnaps.flatSubscribeValues().debugPretty.runDrain
    } yield ()
  }

  "Deploy a simple Flink cluster to Kubernetes" in unsafeRun {
    val spec = FlinkDeploymentDef(
      name = "simple-session",
      namespace = "fdev",
      image = "flink:1.16",
      flinkVersion = FlinkVersion.V1_16,
      jobManager = JobManagerDef(cpu = 1, memory = "1024m"),
      taskManager = TaskManagerDef(cpu = 1, memory = "1024m")
    )
    for {
      _ <- FlinkK8sOperator.deployCluster(114515, spec)
      _ <- FlinkK8sObserver.clusterMetricsSnaps.flatSubscribeValues().debugPretty.runDrain
    } yield ()
  }

  "Deploy a simple Flink session mode job to Kubernetes" in unsafeRun {
    val spec = FlinkSessionJobDef(
      namespace = "fdev",
      name = "simple-sessionjob",
      deploymentName = "simple-session",
      job = JobDef(
        jarURI = s"$assetPath/StateMachineExample.jar",
        parallelism = 1
      )
    )
    for {
      _ <- FlinkK8sOperator.deploySessionJob(114515, spec)
      _ <- FlinkK8sObserver.evaluatedJobSnaps.flatSubscribeValues().debugPretty("evaluated job status").runDrain
    } yield ()
  }

  "Deploy an application mode job with additional jar resources such as third-party dependencies pr udf" in unsafeRun {
    val spec = FlinkDeploymentDef(
      name = "appjob-with-extra-jar",
      namespace = "fdev",
      image = "flink:1.16",
      flinkVersion = FlinkVersion.V1_16,
      jobManager = JobManagerDef(cpu = 1, memory = "1024m"),
      taskManager = TaskManagerDef(cpu = 1, memory = "1024m"),
      job = JobDef(
        jarURI = "assets/quick-sql-1.0.jar",
        parallelism = 1,
        entryClass = "demo.flink.SqlFakerDataJob"
      ),
      extJarPaths = Array("assets/flink-faker-0.5.3.jar")
    )
    for {
      _ <- FlinkK8sOperator.deployApplicationJob(114514, spec)
      _ <- FlinkK8sObserver.evaluatedJobSnaps.flatSubscribeValues().debugPretty.runDrain
    } yield ()
  }

  "Deploy an session mode job with additional jar resources such as third-party dependencies pr udf." in unsafeRun {
    val clusterSpec = FlinkDeploymentDef(
      name = "session-with-extra-jar",
      namespace = "fdev",
      image = "flink:1.16",
      flinkVersion = FlinkVersion.V1_16,
      jobManager = JobManagerDef(cpu = 1, memory = "1024m"),
      taskManager = TaskManagerDef(cpu = 1, memory = "1024m"),
      extJarPaths = Array("assets/flink-faker-0.5.3.jar")
    )
    val jobSpec     = FlinkSessionJobDef(
      namespace = "fdev",
      name = "sessionjob-with-extra-jar",
      deploymentName = "session-with-extra-jar",
      job = JobDef(
        jarURI = "assets/quick-sql-1.0.jar",
        parallelism = 1,
        entryClass = "demo.flink.SqlFakerDataJob"
      )
    )
    for {
      // deploy cluster
      _ <- FlinkK8sOperator.deployCluster(114514, clusterSpec)
      // deploy jar
      _ <- FlinkK8sOperator.deploySessionJob(114514, jobSpec)
      _ <- FlinkK8sObserver.evaluatedJobSnaps.flatSubscribeValues().debugPretty.runDrain
    } yield ()
  }

  "Deploy an application job and set up ingress resources." in unsafeRun {
    val spec = FlinkDeploymentDef(
      name = "appjob-with-ingress",
      namespace = "fdev",
      image = "flink:1.16",
      flinkVersion = FlinkVersion.V1_16,
      jobManager = JobManagerDef(cpu = 1, memory = "1024m"),
      taskManager = TaskManagerDef(cpu = 1, memory = "1024m"),
      job = JobDef(
        jarURI = "local:///opt/flink/examples/streaming/StateMachineExample.jar",
        parallelism = 1
      ),
      ingress = IngressDef.simplePathBased
    )
    for {
      _ <- FlinkK8sOperator.deployApplicationJob(114514, spec)
      _ <- FlinkK8sObserver.evaluatedJobSnaps.flatSubscribeValues().debugPretty.runDrain
    } yield ()
  }

  "Cancel a Flink job" in unsafeRun {
    for {
      _ <- FlinkK8sObserver.track(TrackKey.appJob(114514, "fdev", "simple-appjob"))
      _ <-
        FlinkK8sObserver.evaluatedJobSnaps
          .flatSubscribeValues()
          .takeUntil(snap => snap.clusterNs == "fdev" && snap.clusterId == "simple-appjob" && snap.jobStatus.nonEmpty)
          .runDrain

      _ <- Console.printLine("start to cancel job.")
      _ <- FlinkK8sOperator.cancelJob(114514)
      _ <- Console.printLine("job cancelled")
      _ <- ZIO.interrupt
    } yield ()
  }

  "Stop the flink job and specify the corresponding savepoint configuration" in unsafeRun {
    for {
      _ <- FlinkK8sObserver.track(TrackKey.appJob(114514, "fdev", "simple-appjob"))
      _ <-
        FlinkK8sObserver.evaluatedJobSnaps
          .flatSubscribeValues()
          .takeUntil(snap => snap.clusterNs == "fdev" && snap.clusterId == "simple-appjob" && snap.jobStatus.nonEmpty)
          .runDrain

      _ <- Console.printLine("start to stop job.")
      _ <- FlinkK8sOperator
             .stopJob(114514, JobSavepointDef(savepointPath = "file:///opt/flink/savepoint"))
             .map(_.prettyStr)
             .debug("trigger status result")
      _ <- Console.printLine("job stopped.")
      _ <- ZIO.interrupt.ignore
    } yield ()
  }

  "Trigger savepoint for flink job." in unsafeRun {
    for {
      _ <- FlinkK8sObserver.track(TrackKey.appJob(114514, "fdev", "simple-appjob"))
      _ <-
        FlinkK8sObserver.evaluatedJobSnaps
          .flatSubscribeValues()
          .takeUntil(snap => snap.clusterNs == "fdev" && snap.clusterId == "simple-appjob" && snap.jobStatus.nonEmpty)
          .runDrain

      _ <- Console.printLine("start to stop job.")
      _ <- FlinkK8sOperator
             .triggerJobSavepoint(114514, JobSavepointDef(savepointPath = "file:///opt/flink/savepoint"))
             .map(_.prettyStr)
             .debug("trigger status result")
      _ <- Console.printLine("job stopped.")
      _ <- ZIO.interrupt.ignore
    } yield ()
  }

  "Delete Flink cluster on Kubernetes" in unsafeRun {
    FlinkK8sOperator.k8sCrOpr.deleteDeployment("fdev", "simple-session")
    // or FlinkK8sOperator.delete(114514)
  }

  "Delete a Flink application mode job on Kubernetes" in unsafeRun {
    FlinkK8sOperator.k8sCrOpr.deleteDeployment("fdev", "simple-appjob")
    // or FlinkK8sOperator.delete(114514)
  }

  "Delete flink session mode job resources on kubernetes." in unsafeRun {
    FlinkK8sOperator.k8sCrOpr.deleteSessionJob("fdev", "simple-sessionjob")
    // or FlinkK8sOperator.delete(114514)
  }

  override protected def beforeAll(): Unit = {
    prepareTestAssets()
    EmbeddedFileServer.launch.runIO
  }

}
