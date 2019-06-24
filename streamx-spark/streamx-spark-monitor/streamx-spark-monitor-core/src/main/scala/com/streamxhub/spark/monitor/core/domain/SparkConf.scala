package com.streamxhub.spark.monitor.core.domain


import lombok.Data
import javax.validation.constraints.NotBlank
import java.io.Serializable
import java.util.Date

@Data
class SparkConf(
                 @NotBlank(message = "{required}") val appName: String,
                 @NotBlank(message = "{required}") val confVersion: String,
                 @NotBlank(message = "{required}") var conf: String
               ) extends Serializable {

  var confId: Long = 0L
  var createTime: Date = null
  var modifyTime: Date = null

  private val createTimeFrom = null
  private val createTimeTo = null

}
