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

package org.apache.streampark.flink.kubernetes.model

import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode

/** flink cluster identifier on kubernetes */
case class ClusterKey(
    executeMode: FlinkK8sDeployMode.Value,
    namespace: String = "default",
    clusterId: String) {

  override def toString: String = executeMode.toString + namespace + clusterId

  override def hashCode(): Int =
    Utils.hashCode(executeMode, namespace, clusterId)

  override def equals(obj: Any): Boolean = {
    obj match {
      case that: ClusterKey =>
        this.executeMode == that.executeMode &&
        this.namespace == that.namespace &&
        this.clusterId == that.clusterId
      case _ => false
    }
  }

}

object ClusterKey {
  def of(trackId: TrackId): ClusterKey =
    ClusterKey(trackId.executeMode, trackId.namespace, trackId.clusterId)
}
