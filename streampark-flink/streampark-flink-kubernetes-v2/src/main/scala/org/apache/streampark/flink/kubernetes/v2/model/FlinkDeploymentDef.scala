package org.apache.streampark.flink.kubernetes.v2.model

import org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.FlinkDeployment
import org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.{FlinkDeploymentSpec, FlinkVersion, IngressSpec, JobManagerSpec, JobSpec, JobState, KubernetesDeploymentMode, Resource, TaskManagerSpec, UpgradeMode}

import io.fabric8.kubernetes.api.model.{ObjectMeta, Pod}

import scala.jdk.CollectionConverters.mapAsJavaMapConverter

/**
 * Flink Application mode job CR definition.
 * Typed-safe Mirror of [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkDeploymentSpec]]
 *
 * @param namespace K8s CR namespace
 * @param name K8s CR name
 * @param image Base Flink image
 * @param imagePullPolicy Image pull policy, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkDeploymentSpec#imagePullPolicy]]
 * @param serviceAccount K8s account nane, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkDeploymentSpec#serviceAccount]]
 * @param flinkVersion Target Flink version, see [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkDeploymentSpec#flinkVersion]]
 * @param jobManager JobManager definition
 * @param taskManager TaskManager definition
 * @param restartNonce See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.AbstractFlinkSpec#restartNonce]]
 * @param flinkConfiguration Extra Flink original configuration
 * @param logConfiguration Extra Flink log configuration
 * @param podTemplate Extra Pod Template definition, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkDeploymentSpec#podTemplate]]
 * @param ingress Ingress definition.
 * @param mode See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.KubernetesDeploymentMode]]
 * @param job Job definition
 * @param extJarPaths Additional jar dependencies path, only allows local paths likes "/streampark/ws/assets/flink-faker-0.5.3.jar"
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
    mode: KubernetesDeploymentMode = KubernetesDeploymentMode.NATIVE,
    job: Option[JobDef] = None,
    extJarPaths: Array[String] = Array.empty) {

  def toFlinkDeployment: FlinkDeployment = {
    val spec = new FlinkDeploymentSpec()

    spec.setImage(image)
    imagePullPolicy.foreach(spec.setImagePullPolicy)
    spec.setServiceAccount(serviceAccount)
    spec.setFlinkVersion(flinkVersion)
    restartNonce.foreach(spec.setRestartNonce(_))
    podTemplate.foreach(spec.setPodTemplate)
    spec.setMode(mode)

    val jmSpec = jobManager.toJobManagerSpec
    spec.setJobManager(jmSpec)
    val tmSpec = taskManager.toTaskManagerSpec
    spec.setTaskManager(tmSpec)

    if (flinkConfiguration.nonEmpty) spec.setFlinkConfiguration(flinkConfiguration.asJava)
    if (logConfiguration.nonEmpty) spec.setLogConfiguration(logConfiguration.asJava)

    ingress.map(_.toIngressSpec).foreach(spec.setIngress)
    job.map(_.toJobSpec).foreach(spec.setJob)

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

/**
 * JobManager definition.
 *
 * @param cpu cpu core, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.Resource]]
 * @param memory memory size, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.Resource]]
 * @param ephemeralStorage ephemeral storage size, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.Resource]]
 * @param replicas replicas number, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.JobManagerSpec]]
 * @param podTemplate Pod Template definition
 */
case class JobManagerDef(
    cpu: Double,
    memory: String,
    ephemeralStorage: Option[String] = None,
    replicas: Int = 1,
    podTemplate: Option[Pod] = None) {

  def toJobManagerSpec: JobManagerSpec = {
    val spec = new JobManagerSpec()
    spec.setResource(new Resource(cpu, memory, ephemeralStorage.orNull))
    spec.setReplicas(replicas)
    podTemplate.foreach(spec.setPodTemplate)
    spec
  }
}

/**
 * Taskmanager definition.
 *
 * @param cpu cpu core, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.Resource]]
 * @param memory memory size, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.Resource]]
 * @param ephemeralStorage ephemeral storage size, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.Resource]]
 * @param replicas replicas number, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.TaskManagerSpec]]
 * @param podTemplate Pod Template definition
 */
case class TaskManagerDef(
    cpu: Double,
    memory: String,
    ephemeralStorage: Option[String] = None,
    replicas: Option[Int] = None,
    podTemplate: Option[Pod] = None) {

  def toTaskManagerSpec: TaskManagerSpec = {
    val spec = new TaskManagerSpec()
    spec.setResource(new Resource(cpu, memory, ephemeralStorage.orNull))
    replicas.foreach(spec.setReplicas(_))
    podTemplate.foreach(spec.setPodTemplate)
    spec
  }
}

/**
 * Ingress definition for JobManager.
 * There are two predefined definitions:
 *  - [[org.apache.streampark.flink.kubernetes.v2.model.IngressDef#simplePathBased]]
 *  - [[org.apache.streampark.flink.kubernetes.v2.model.IngressDef#simpleDomainBased]]
 *
 * @param template  ingress template name, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.IngressSpec#template]]
 * @param className ingress class name, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.IngressSpec#className]]
 * @param annotations ingress annotation, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.IngressSpec#annotations]]
 */
case class IngressDef(
    template: String,
    className: Option[String] = None,
    annotations: Map[String, String] = Map.empty) {

  def toIngressSpec: IngressSpec = {
    val spec = new IngressSpec()
    spec.setTemplate(template)
    className.foreach(spec.setClassName)
    if (annotations.nonEmpty) spec.setAnnotations(annotations.asJava)
    spec
  }
}

object IngressDef {
  def simplePathBased: IngressDef = IngressDef(
    template = "/{{namespace}}/{{name}}(/|$)(.*)",
    annotations = Map("nginx.ingress.kubernetes.io/rewrite-target" -> "/$2")
  )

  def simpleDomainBased: IngressDef = IngressDef(
    template = "{{name}}.{{namespace}}.flink.k8s.io"
  )
}

/**
 * Job definition.
 *
 * @param jarURI JAR file URI that only supports local file likes "/streampark/ws/assets/flink-faker-0.5.3.jar"
 * @param parallelism job parallelism
 * @param entryClass job entry class
 * @param args job arguments
 * @param state Expected job state, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.JobState]]
 * @param upgradeMode Job update mode, See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.UpgradeMode]]
 * @param savepointTriggerNonce See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.JobSpec#setSavepointTriggerNonce]]
 * @param initialSavepointPath See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.JobSpec#setInitialSavepointPath]]
 * @param allowNonRestoredState See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.JobSpec#setAllowNonRestoredState]]
 */
case class JobDef(
    jarURI: String,
    parallelism: Int,
    entryClass: Option[String] = None,
    args: Array[String] = Array.empty,
    state: JobState = org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.JobState.RUNNING,
    upgradeMode: UpgradeMode = UpgradeMode.STATELESS,
    savepointTriggerNonce: Option[Long] = None,
    initialSavepointPath: Option[String] = None,
    allowNonRestoredState: Option[Boolean] = None) {

  def toJobSpec: JobSpec = {
    val spec = new JobSpec()
    spec.setJarURI(jarURI)
    spec.setParallelism(parallelism)
    entryClass.foreach(spec.setEntryClass)
    if (args.nonEmpty) spec.setArgs(args)
    spec.setState(state)
    spec.setUpgradeMode(upgradeMode)

    savepointTriggerNonce.foreach(spec.setSavepointTriggerNonce(_))
    initialSavepointPath.foreach(spec.setInitialSavepointPath)
    allowNonRestoredState.foreach(spec.setAllowNonRestoredState(_))
    spec
  }
}
