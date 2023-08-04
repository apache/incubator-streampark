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

package org.apache.streampark.flink.kubernetes.v2.operator

import org.apache.streampark.flink.kubernetes.v2.{pathLastSegment, yamlMapper}
import org.apache.streampark.flink.kubernetes.v2.K8sTools.usingK8sClient
import org.apache.streampark.flink.kubernetes.v2.fs.FileMirror
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkDeploymentDef, FlinkSessionJobDef, JobDef}
import org.apache.streampark.flink.kubernetes.v2.observer.FlinkK8sObserver

import io.fabric8.kubernetes.api.model._
import org.apache.flink.v1beta1.{FlinkDeployment, FlinkSessionJob}
import zio.{IO, UIO, ZIO}
import zio.stream.ZStream

import java.util

import scala.collection.convert.ImplicitConversions._
import scala.jdk.CollectionConverters._

/**
 * Flink Kubernetes CR operator.
 * Responsible for applying and deleting operations on CR resources.
 */
sealed trait CROperator {

  /** Apply FlinkDeployment CR. */
  def applyDeployment(spec: FlinkDeploymentDef): IO[Throwable, Unit]

  /** Apply FlinkSessionJob CR. */
  def applySessionJob(spec: FlinkSessionJobDef): IO[Throwable, Unit]

  /** Delete FlinkDeployment CR. */
  def deleteDeployment(namespace: String, name: String): IO[Throwable, Unit]

  /** Delete FlinkSessionJob CR. */
  def deleteSessionJob(namespace: String, name: String): IO[Throwable, Unit]

}

object CROperator extends CROperator {

  /** Apply FlinkDeployment CR. */
  // noinspection DuplicatedCode
  override def applyDeployment(spec: FlinkDeploymentDef): IO[Throwable, Unit] = {
    lazy val mirrorSpace = s"${spec.namespace}_${spec.name}"
    for {
      // Generate FlinkDeployment CR
      correctedJob        <- mirrorJobJarToHttpFileServer(spec.job, mirrorSpace)
      correctedExtJars    <- mirrorExtJarsToHttpFileServer(spec.extJarPaths, mirrorSpace)
      correctedPod        <- correctPodSpec(
                               spec.podTemplate,
                               correctedExtJars ++ correctedJob.map(_.jarURI).filter(_.startsWith("http://")).toArray[String]
                             )
      correctedLocalUriJob = correctedJob.map { jobDef =>
                               if (!jobDef.jarURI.startsWith("http://")) jobDef
                               else jobDef.copy("local:///opt/flink/lib/" + pathLastSegment(jobDef.jarURI))
                             }
      correctedSpec        = spec.copy(
                               job = correctedLocalUriJob,
                               extJarPaths = correctedExtJars,
                               podTemplate = correctedPod
                             )
      flinkDeployCR        = correctedSpec.toFlinkDeployment
      // Logging CR yaml
      _                   <- ZIO
                               .attempt(yamlMapper.writeValueAsString(flinkDeployCR))
                               .catchAll(e => ZIO.succeed(e.getMessage))
                               .flatMap(yaml => ZIO.logInfo(s"Applying FlinkDeployment K8s CR: \n$yaml"))
                               .when(logFlinkCrYaml)

      // Apply FlinkDeployment CR to kubernetes
      isCrExist           <- FlinkK8sObserver.getFlinkDeploymentCrSpec(spec.namespace, spec.name).map(_.isDefined)
      _                   <- usingK8sClient { client =>
                               if (isCrExist) client.resource(flinkDeployCR).update()
                               else client.resource(flinkDeployCR).create()
                             }
    } yield ()
  } *> ZIO.logInfo(s"Successfully apply FlinkDeployment K8s CR: namespace=${spec.namespace}, name=${spec.name}")

  /** Apply FlinkSessionJob CR. */
  // noinspection DuplicatedCode
  def applySessionJob(spec: FlinkSessionJobDef): IO[Throwable, Unit] = {
    lazy val mirrorSpace = s"${spec.namespace}_${spec.name}"
    for {
      // Generate FlinkSessionJob CR
      correctedJob <- mirrorJobJarToHttpFileServer(Some(spec.job), mirrorSpace).map(_.get)
      correctedSpec = spec.copy(job = correctedJob)
      sessionJobCR  = correctedSpec.toFlinkSessionJob
      // Logging CR yaml
      _            <- ZIO
                        .attempt(yamlMapper.writeValueAsString(sessionJobCR))
                        .catchAll(e => ZIO.succeed(e.getMessage))
                        .flatMap(yaml => ZIO.logInfo(s"Applying FlinkSessionJob K8s CR: \n$yaml"))
                        .when(logFlinkCrYaml)

      // Apply FlinkSessionJob CR to kubernetes
      isCrExist    <- FlinkK8sObserver.getFlinkSessionJobCrSpec(spec.namespace, spec.name).map(_.isDefined)
      _            <- usingK8sClient { client =>
                        if (isCrExist) client.resource(sessionJobCR).update()
                        else client.resource(sessionJobCR).create()
                      }
    } yield ()
  } *> ZIO.logInfo(s"Successfully apply FlinkSessionJob K8s CR: namespace=${spec.namespace}, name=${spec.name}")

  // Convert job.uri to file-server http access uri
  private def mirrorJobJarToHttpFileServer(job: Option[JobDef], mirrorSpace: String) = {
    for {
      jobJarHttpUrl <- job
                         .map(_.jarURI)
                         .filter(!_.startsWith("local://"))
                         .map(jarUri => FileMirror.mirrorAndGetHttpUrl(jarUri, mirrorSpace).map(Some(_)))
                         .getOrElse(ZIO.succeed(None))

      correctedJob = jobJarHttpUrl match {
                       case Some(url) => job.map(_.copy(jarURI = url))
                       case None      => job
                     }
    } yield correctedJob
  }

  // Convert extra jar paths to file-server http access uri.
  private def mirrorExtJarsToHttpFileServer(extJars: Array[String], mirrorSpace: String) = {
    ZStream
      .fromIterable(extJars)
      .mapZIOPar(5)(path => FileMirror.mirrorAndGetHttpUrl(path, mirrorSpace))
      .runCollect
      .map(_.toArray)
  }

  // Inject pod-template to load jars from http file server.
  private def correctPodSpec(oriPod: Option[Pod], jarHttpUrls: Array[String]): UIO[Option[Pod]] = ZIO.succeed {
    if (jarHttpUrls.isEmpty) oriPod
    else {
      val pod = oriPod.getOrElse(new Pod())

      // handle metadata
      val metadata = Option(pod.getMetadata).getOrElse(new ObjectMeta())
      metadata.setName("pod-template")
      pod.setMetadata(metadata)

      val spec = Option(pod.getSpec).getOrElse(new PodSpec())

      // handle initContainers
      val initContainers: util.List[Container] = Option(spec.getInitContainers).getOrElse(new util.ArrayList())

      val libLoaderInitContainer = new ContainerBuilder()
        .withName("userlib-loader")
        .withImage("busybox:1.35.0")
        .withCommand(
          "sh",
          "-c",
          jarHttpUrls.map(url => s"wget $url -O /opt/flink/lib/${pathLastSegment(url)}").mkString(" && "))
        .withVolumeMounts(
          new VolumeMountBuilder()
            .withName("flink-usrlib")
            .withMountPath("/opt/flink/lib")
            .build
        )
        .build

      initContainers.add(libLoaderInitContainer)

      spec.setInitContainers(initContainers)

      // handle containers
      val flinkMainContainerVolMounts: util.List[VolumeMount] =
        jarHttpUrls
          .map(url => pathLastSegment(url))
          .map(jarName =>
            new VolumeMountBuilder()
              .withName("flink-usrlib")
              .withMountPath(s"/opt/flink/lib/$jarName")
              .withSubPath(jarName)
              .build)
          .toList

      val containers: util.List[Container] = Option(spec.getContainers).getOrElse(new util.ArrayList())

      containers.zipWithIndex
        .find { case (e, _) => e.getName == "flink-main-container" }
        .map { case (e, idx) =>
          val volMounts = Option(e.getVolumeMounts)
            .map { mounts =>
              mounts.addAll(flinkMainContainerVolMounts)
              mounts
            }
            .getOrElse(flinkMainContainerVolMounts)
          e.setVolumeMounts(volMounts)
          containers.set(idx, e)
        }
        .getOrElse(
          containers.add(
            new ContainerBuilder()
              .withName("flink-main-container")
              .withVolumeMounts(flinkMainContainerVolMounts)
              .build)
        )

      spec.setContainers(containers)

      // handle volumes
      val volumes: util.List[Volume] = Option(spec.getVolumes).getOrElse(new util.ArrayList())
      volumes.add(
        new VolumeBuilder()
          .withName("flink-usrlib")
          .withEmptyDir(new EmptyDirVolumeSource())
          .build
      )
      spec.setVolumes(volumes)

      pod.setSpec(spec)
      Some(pod)
    }
  }

  /** Delete FlinkDeployment CR. */
  def deleteDeployment(namespace: String, name: String): IO[Throwable, Unit] =
    usingK8sClient { client =>
      client
        .resources(classOf[FlinkDeployment])
        .inNamespace(namespace)
        .withName(name)
        .delete()
    } *> ZIO.logInfo(s"Delete FlinkDeployment CR: namespace=$namespace, name=$name")

  /** Delete FlinkSessionJob CR. */
  def deleteSessionJob(namespace: String, name: String): IO[Throwable, Unit] =
    usingK8sClient { client =>
      client
        .resources(classOf[FlinkSessionJob])
        .inNamespace(namespace)
        .withName(name)
        .delete()
    } *> ZIO.logInfo(s"Delete FlinkDeployment CR: namespace=$namespace, name=$name")

}
