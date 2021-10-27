package com.streamxhub.streamx.flink.submit.domain

import java.util.{Map => JavaMap}
import javax.annotation.Nullable

case class SubmitResponse(clusterId: String,
                          flinkConfig: JavaMap[String, String],
                          @Nullable jobId: String = "") {

}
