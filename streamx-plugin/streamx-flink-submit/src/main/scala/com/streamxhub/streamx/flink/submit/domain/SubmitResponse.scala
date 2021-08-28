package com.streamxhub.streamx.flink.submit.domain

import org.apache.flink.configuration.Configuration

import javax.annotation.Nullable

case class SubmitResponse(clusterId: String,
                          flinkConfig: Configuration,
                          @Nullable jobId: String = "") {

}
