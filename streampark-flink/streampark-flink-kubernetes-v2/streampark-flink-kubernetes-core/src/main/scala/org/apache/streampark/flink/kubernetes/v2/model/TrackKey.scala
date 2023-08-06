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

/** Flink kubernetes resource tracking id which is a unique value defined in the StreamPark system. */
sealed trait TrackKey {

  /** id attribute depends on the specific resource. */
  val id: Long

  /** flink cluster namespace on kubernetes */
  def clusterNamespace: String

  /** flink cluster name on kubernetes */
  def clusterName: String
}

object TrackKey {

  def appJob(id: Long, namespace: String, name: String): ApplicationJobKey = ApplicationJobKey(id, namespace, name)

  def sessionJob(id: Long, namespace: String, name: String, clusterName: String): SessionJobKey =
    SessionJobKey(id, namespace, name, clusterName)

  def cluster(id: Long, namespace: String, name: String): ClusterKey = ClusterKey(id, namespace, name)

  def unmanagedSessionJob(id: Long, clusterNs: String, clusterName: String, jid: String): UnmanagedSessionJobKey =
    UnmanagedSessionJobKey(id, clusterNs, clusterName, jid)

  /**
   * Key of Flink application mode Job.
   * It is also compatible with application mode tasks managed by the flink k8s operator
   * or directly by streampark in previous versions.
   *
   * @param id        ref to [[org.apache.streampark.console.core.entity.Application.id]]
   * @param namespace k8s CR namespace
   * @param name      k8s CR name
   */
  case class ApplicationJobKey(id: Long, namespace: String, name: String) extends TrackKey {
    lazy val clusterNamespace = namespace
    lazy val clusterName      = name
  }

  /**
   * Key of Flink session mode Job that manged by Flink K8s operator.
   *
   * @param id          ref to [[org.apache.streampark.console.core.entity.Application.id]]
   * @param namespace   k8s CR namespace
   * @param name        k8s CR namespace
   * @param clusterName The CR name of the target flink cluster submitted by the job.
   */
  case class SessionJobKey(id: Long, namespace: String, name: String, clusterName: String) extends TrackKey {
    lazy val clusterNamespace = namespace
  }

  /**
   * Key of Flink cluster manged by Flink K8s operator.
   *
   * @param id        ref to [[org.apache.streampark.console.core.entity.FlinkCluster.id]]
   * @param namespace k8s CR namespace
   * @param name      k8s CR namespace
   */
  case class ClusterKey(id: Long, namespace: String, name: String) extends TrackKey {
    lazy val clusterNamespace = namespace
    lazy val clusterName      = name
  }

  /**
   * Compatible with previous versions of tasks submitted directly to flink-k8s-session.
   *
   * @param id        ref to [[org.apache.streampark.console.core.entity.Application.id]]]
   * @param namespace flink cluster k8s namespace
   * @param clusterId flink cluster k8s name
   * @param jid       jobid
   */
  case class UnmanagedSessionJobKey(id: Long, namespace: String, clusterId: String, jid: String) extends TrackKey {
    lazy val clusterNamespace = namespace
    lazy val clusterName      = clusterId
  }

}
