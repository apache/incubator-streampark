package com.streamxhub.spark.monitor.core.domain

import java.io.Serializable
import java.util.Date

import javax.validation.constraints.NotBlank
import lombok.Data

@Data
class SparkMonitor(
                    @NotBlank(message = "{required}") val monitorId: Int,
                    @NotBlank(message = "{required}") val appId: String,
                    @NotBlank(message = "{required}") val appName: String,
                    @NotBlank(message = "{required}") val confVersion: String
                  ) extends Serializable {
  var status: Int = 0
  var createTime: Date = null
  var modifyTime: Date = null

  def this() {
    this(0, null, null, null)
  }

}






