/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.submit.bean

import java.io.File
import java.util.{Map => JavaMap}

import com.streamxhub.streamx.common.conf.Workspace
import javax.annotation.Nullable
import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.enums.{ExecutionMode, FlinkK8sRestExposedType, ResolveOrder}
import com.streamxhub.streamx.common.util.FlinkUtils
import org.apache.commons.io.FileUtils
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

case class DeployRequest(flinkVersion: FlinkVersion,
                         clusterId: String,
                         executionMode: ExecutionMode,
                         resolveOrder: ResolveOrder,
                         flameGraph: JavaMap[String, java.io.Serializable],
                         dynamicOption: JavaMap[String, String],
                         @Nullable k8sDeployParam: KubernetesDeployParam,
                         @Nullable extraParameter: JavaMap[String, Any]
                         ) {
  private[submit] lazy val hdfsWorkspace = {
    /**
      * 必须保持本机flink和hdfs里的flink版本和配置都完全一致.
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

