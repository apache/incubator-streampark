package com.streamxhub.streamx.flink.submit.domain

import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

case class StopRequest(flinkHome: String,
                       clusterId: String,
                       jobId: String,
                       withSavePoint: Boolean,
                       withDrain: Boolean,
                       kubernetesNamespace: String = KubernetesConfigOptions.NAMESPACE.defaultValue()) {

}
