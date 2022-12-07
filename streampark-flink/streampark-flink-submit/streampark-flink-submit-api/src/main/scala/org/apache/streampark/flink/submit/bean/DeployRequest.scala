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

package org.apache.streampark.flink.submit.bean

import java.io.File
import java.util.{Map => JavaMap}

import org.apache.streampark.common.conf.Workspace
import javax.annotation.Nullable
import org.apache.streampark.common.domain.FlinkVersion
import org.apache.streampark.common.enums.{ExecutionMode, FlinkK8sRestExposedType}
import org.apache.streampark.common.util.FlinkUtils
import org.apache.commons.io.FileUtils
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

case class DeployRequest(flinkVersion: FlinkVersion,
                         clusterId: String,
                         executionMode: ExecutionMode,
                         properties: JavaMap[String, Any],
                         @Nullable k8sDeployParam: KubernetesDeployParam) {

  private[submit] lazy val hdfsWorkspace = {
    /**
      * You must keep the flink version and configuration in the native flink and hdfs exactly the same.
      */
    val workspace = Workspace.remote
    val flinkHome = flinkVersion.flinkHome
    val flinkHomeDir = new File(flinkHome)
    val flinkName = if (FileUtils.isSymlink(flinkHomeDir)) {
      flinkHomeDir.getCanonicalFile.getName
    } else {
      flinkHomeDir.getName
    }
    val flinkHdfsHome = s"${workspace.APP_FLINK}/$flinkName"
    HdfsWorkspace(
      flinkName,
      flinkHome,
      flinkLib = s"$flinkHdfsHome/lib",
      flinkPlugins = s"$flinkHdfsHome/plugins",
      flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome),
      appJars = workspace.APP_JARS,
      appPlugins = workspace.APP_PLUGINS
    )
  }
}

case class KubernetesDeployParam(clusterId: String,
                                 kubernetesNamespace: String = KubernetesConfigOptions.NAMESPACE.defaultValue(),
                                 kubeConf: String = "~/.kube/config",
                                 serviceAccount: String = KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT.defaultValue(),
                                 flinkImage: String = KubernetesConfigOptions.CONTAINER_IMAGE.defaultValue(),
                                 @Nullable flinkRestExposedType: FlinkK8sRestExposedType = FlinkK8sRestExposedType.ClusterIP)

