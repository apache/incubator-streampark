package org.apache.streampark.console.core.service.Impl

import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.flink.client.bean.{DeployRequest, ShutDownRequest}
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator

import scala.util.{Failure, Success}

object FlinkClusterServiceImplV2 extends Logger {

  @throws[Throwable]
  def shutdown(shutDownRequest: ShutDownRequest): Unit = {
    val name      = shutDownRequest.clusterId
    val namespace = shutDownRequest.kubernetesDeployParam.kubernetesNamespace

    def richMsg: String => String = s"[flink-shutdown][clusterId=$name][namespace=$namespace] " + _

    FlinkK8sOperator.k8sCrOpr.deleteSessionJob(namespace, name).runIOAsTry match {
      case Success(_)   =>
        logInfo(richMsg("Shutdown Flink cluster successfully."))
      case Failure(err) =>
        logError(richMsg(s"Fail to shutdown Flink cluster"), err)
        throw err
    }
  }

}
