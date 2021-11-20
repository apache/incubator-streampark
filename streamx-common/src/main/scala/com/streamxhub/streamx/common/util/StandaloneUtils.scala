package com.streamxhub.streamx.common.util

import org.apache.flink.configuration.JobManagerOptions

import scala.collection.immutable.Map

object StandaloneUtils {

  val DEFAULT_JM_ADDRESS: String = "localhost"
  val DEFAULT_JM_PORT: Integer = 8080

  def getRMWebAppURL(dynamicOptions: Map[String, String], flinkUrl: String): String = {
    val address = dynamicOptions.getOrElse(JobManagerOptions.ADDRESS.key, () => DEFAULT_JM_ADDRESS)
    val port = dynamicOptions.getOrElse(JobManagerOptions.PORT.key, () => DEFAULT_JM_PORT)
    "https://" + address + ":" + port + "/" + flinkUrl
  }
}
