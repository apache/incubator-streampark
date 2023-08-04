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

package org.apache.streampark.flink.kubernetes.v2.model

import org.apache.streampark.flink.kubernetes.v2.jacksonMapper
import org.apache.streampark.flink.kubernetes.v2.model.FlinkDeploymentDef.mapPodToPodTemplate

import io.fabric8.kubernetes.api.model.{ObjectMeta, Pod}
import org.apache.flink.v1beta1.{flinkdeploymentspec, FlinkDeployment, FlinkDeploymentSpec}
import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion
import org.apache.flink.v1beta1.flinkdeploymentspec.{Ingress, TaskManager}

import scala.jdk.CollectionConverters.mapAsJavaMapConverter
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

/**
 * Flink Deployment CR definition for application mode job or session cluster.
 * Typed-safe Mirror of [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkDeploymentSpec]]
 *
 * @param namespace          K8s CR namespace
 * @param name               K8s CR name
 * @param image              Flink docker image used to start the Job and TaskManager pods.
 * @param imagePullPolicy    Image pull policy of the Flink docker image.
 * @param serviceAccount     Kubernetes service used by the Flink deployment.
 * @param flinkVersion        Flink image version.
 * @param jobManager         Flink JobManager definition.
 * @param taskManager        Flink TaskManager definition.
 * @param restartNonce       Nonce used to manually trigger restart for the cluster/session job. In order to trigger restart,
 *                           change the number to anything other than the current value.
 * @param flinkConfiguration   Flink configuration overrides for the Flink deployment or Flink session job.
 * @param logConfiguration    Log configuration overrides for the Flink deployment. Format logConfigFileName -> configContent.
 * @param podTemplate        Base pod template for job and task manager pods. Can be overridden by the jobManager and taskManager pod templates.
 * @param ingress            Ingress definition.
 * @param mode               Deployment mode of the Flink cluster, native or standalone, default: native.
 * @param job                Job definition for application deployments/session job. Null for session clusters.
 * @param extJarPaths        Additional jar dependencies path, only allows local paths likes "/streampark/ws/assets/flink-faker-0.5.3.jar"
 */

case class FlinkDeploymentDef(
    namespace: String,
    name: String,
    image: String,
    imagePullPolicy: Option[String] = None,
    serviceAccount: String = "flink",
    flinkVersion: FlinkVersion,
    jobManager: JobManagerDef,
    taskManager: TaskManagerDef,
    restartNonce: Option[Long] = None,
    flinkConfiguration: Map[String, String] = Map.empty,
    logConfiguration: Map[String, String] = Map.empty,
    podTemplate: Option[Pod] = None,
    ingress: Option[IngressDef] = None,
    mode: FlinkDeploymentSpec.Mode = FlinkDeploymentSpec.Mode._NATIVE,
    job: Option[JobDef] = None,
    extJarPaths: Array[String] = Array.empty) {

  // noinspection DuplicatedCode
  def toFlinkDeployment: FlinkDeployment = {
    val spec = new FlinkDeploymentSpec()

    spec.setImage(image)
    imagePullPolicy.foreach(spec.setImagePullPolicy)
    spec.setServiceAccount(serviceAccount)
    spec.setFlinkVersion(flinkVersion)
    restartNonce.foreach(spec.setRestartNonce(_))
    podTemplate
      .flatMap(pod => mapPodToPodTemplate(pod, classOf[flinkdeploymentspec.PodTemplate]).toOption)
      .foreach(spec.setPodTemplate)
    spec.setMode(mode)

    val jmSpec = jobManager.toJobManagerSpec
    spec.setJobManager(jmSpec)
    val tmSpec = taskManager.toTaskManagerSpec
    spec.setTaskManager(tmSpec)

    if (flinkConfiguration.nonEmpty) spec.setFlinkConfiguration(flinkConfiguration.asJava)
    if (logConfiguration.nonEmpty) spec.setLogConfiguration(logConfiguration.asJava)

    ingress.map(_.toIngressSpec).foreach(spec.setIngress)
    job.map(_.toFlinkDeploymentJobSpec).foreach(spec.setJob)

    val deployment = new FlinkDeployment()
    val metadata   = new ObjectMeta()
    metadata.setNamespace(namespace)
    metadata.setName(name)
    deployment.setMetadata(metadata)
    deployment.setSpec(spec)
    deployment.setStatus(null)
    deployment
  }
}

object FlinkDeploymentDef {
  def mapPodToPodTemplate[A: ClassTag](pod: Pod, clz: Class[A]): Try[A] = Try {
    val json = jacksonMapper.writeValueAsString(pod)
    jacksonMapper.readValue(json, clz)
  }
}

/**
 * JobManager definition.
 * Type-safe mirror of [[org.apache.flink.v1beta1.flinkdeploymentspec.JobManager]]
 *
 * @param cpu              Amount of CPU allocated to the pod.
 * @param memory           Amount of memory allocated to the pod. Example: 1024m, 1g
 * @param ephemeralStorage Amount of ephemeral storage allocated to the pod. Example: 1024m, 2G
 * @param replicas         Number of TaskManager replicas. If defined, takes precedence over parallelism
 * @param podTemplate      JobManager pod template.
 */
case class JobManagerDef(
    cpu: Double,
    memory: String,
    ephemeralStorage: Option[String] = None,
    replicas: Int = 1,
    podTemplate: Option[Pod] = None) {

  def toJobManagerSpec: flinkdeploymentspec.JobManager = {
    val spec     = new flinkdeploymentspec.JobManager()
    val resource = new flinkdeploymentspec.jobmanager.Resource().pipe { rs =>
      rs.setCpu(cpu)
      rs.setMemory(memory)
      rs.setEphemeralStorage(ephemeralStorage.orNull)
      rs
    }
    spec.setResource(resource)
    spec.setReplicas(replicas)
    podTemplate
      .flatMap(pod => mapPodToPodTemplate(pod, classOf[flinkdeploymentspec.jobmanager.PodTemplate]).toOption)
      .foreach(spec.setPodTemplate)
    spec
  }
}

/**
 * Taskmanager definition.
 * Type-safe mirror of [[org.apache.flink.v1beta1.flinkdeploymentspec.TaskManager]]
 *
 * @param cpu              Amount of CPU allocated to the pod.
 * @param memory           Amount of memory allocated to the pod. Example: 1024m, 1g
 * @param ephemeralStorage Amount of ephemeral storage allocated to the pod. Example: 1024m, 2G
 * @param replicas         Number of TaskManager replicas. If defined, takes precedence over parallelism
 * @param podTemplate      TaskManager pod template.
 */
case class TaskManagerDef(
    cpu: Double,
    memory: String,
    ephemeralStorage: Option[String] = None,
    replicas: Option[Int] = None,
    podTemplate: Option[Pod] = None) {

  def toTaskManagerSpec: flinkdeploymentspec.TaskManager = {
    val spec     = new TaskManager()
    val resource = new flinkdeploymentspec.taskmanager.Resource().pipe { rs =>
      rs.setCpu(cpu)
      rs.setMemory(memory)
      rs.setEphemeralStorage(ephemeralStorage.orNull)
      rs
    }
    spec.setResource(resource)
    replicas.foreach(spec.setReplicas(_))
    podTemplate
      .flatMap(pod => mapPodToPodTemplate(pod, classOf[flinkdeploymentspec.taskmanager.PodTemplate]).toOption)
      .foreach(spec.setPodTemplate)
    spec
  }
}

/**
 * Ingress definition for JobManager.
 * Type-safe mirror of [[org.apache.flink.v1beta1.flinkdeploymentspec.Ingress]]
 *
 * There are two predefined definitions:
 *  - [[org.apache.streampark.flink.kubernetes.v2.model.FlinkDeploymentDef.IngressDef.simplePathBased]]
 *  - [[org.apache.streampark.flink.kubernetes.v2.model.FlinkDeploymentDef.IngressDef.simpleDomainBased]]
 *
 * @param template    Ingress template name.
 * @param className   Ingress class name.
 * @param annotations Ingress annotation.
 */
case class IngressDef(
    template: String,
    className: Option[String] = None,
    annotations: Map[String, String] = Map.empty) {

  def toIngressSpec: flinkdeploymentspec.Ingress = {
    val spec = new Ingress()
    spec.setTemplate(template)
    className.foreach(spec.setClassName)
    if (annotations.nonEmpty) spec.setAnnotations(annotations.asJava)
    spec
  }
}

object IngressDef {

  lazy val simplePathBased: IngressDef = IngressDef(
    template = "/{{namespace}}/{{name}}(/|$)(.*)",
    annotations = Map("nginx.ingress.kubernetes.io/rewrite-target" -> "/$2")
  )

  lazy val simpleDomainBased: IngressDef = IngressDef(
    template = "{{name}}.{{namespace}}.flink.k8s.io"
  )
}
