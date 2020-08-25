package com.streamxhub.flink.submit

import org.apache.flink.yarn.configuration.YarnDeploymentTarget


case class SubmitInfo(deployMode: YarnDeploymentTarget,
                      nameService: String,
                      flinkUserJar: String,
                      yarnName: String,
                      appConf: String,
                      overrideOption: Array[String],
                      args: String
                     )
