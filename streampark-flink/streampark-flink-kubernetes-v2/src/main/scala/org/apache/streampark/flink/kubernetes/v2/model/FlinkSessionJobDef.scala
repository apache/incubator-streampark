package org.apache.streampark.flink.kubernetes.v2.model

import org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.FlinkSessionJob
import org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.FlinkSessionJobSpec

import io.fabric8.kubernetes.api.model.ObjectMeta

import scala.jdk.CollectionConverters.mapAsJavaMapConverter

/**
 * Flink Session job CR definition.
 * Typed-safe Mirror of [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.FlinkSessionJob]]
 *
 * @param namespace K8s CR namespace
 * @param name K8s CR name
 * @param deploymentName Name of the parent Flink cluster CR
 * @param job Job definition
 * @param flinkConfiguration Extra Flink original configuration
 * @param restartNonce See [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.spec.AbstractFlinkSpec#restartNonce]]
 */
case class FlinkSessionJobDef(
    namespace: String,
    name: String,
    deploymentName: String,
    job: JobDef,
    flinkConfiguration: Map[String, String] = Map.empty,
    restartNonce: Option[Long] = None) {

  def toFlinkSessionJob: FlinkSessionJob = {
    val spec = new FlinkSessionJobSpec()
    spec.setDeploymentName(deploymentName)
    spec.setJob(job.toJobSpec)
    if (flinkConfiguration.nonEmpty) spec.setFlinkConfiguration(flinkConfiguration.asJava)
    restartNonce.foreach(spec.setRestartNonce(_))

    val sessionJob = new FlinkSessionJob()
    val metadata   = new ObjectMeta()
    metadata.setNamespace(namespace)
    metadata.setName(name)
    sessionJob.setMetadata(metadata)
    sessionJob.setSpec(spec)
    sessionJob.setStatus(null)
    sessionJob
  }
}
