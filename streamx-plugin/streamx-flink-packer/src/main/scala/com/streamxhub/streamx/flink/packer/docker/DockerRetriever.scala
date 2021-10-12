package com.streamxhub.streamx.flink.packer.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

import java.time.Duration

/**
 * author: Al-assad
 */
object DockerRetriever {

  /**
   * docker config param from system properties,
   * see in https://github.com/docker-java/docker-java/blob/master/docs/getting_started.md#properties-docker-javaproperties
   * todo support custom docker configuration parameters in unified configurations in the future
   */
  lazy val dockerClientConf: DockerClientConfig = {
    DefaultDockerClientConfig.createDefaultConfigBuilder().build()
  }

  /**
   * docker http client builder, use ApacheDockerHttpClient by default
   * todo support custom http client configuration parameters in unified configurations in the future
   */
  lazy val dockerHttpClientBuilder: ApacheDockerHttpClient.Builder = new ApacheDockerHttpClient.Builder()
    .dockerHost(dockerClientConf.getDockerHost)
    .sslConfig(dockerClientConf.getSSLConfig)
    .maxConnections(100)
    .connectionTimeout(Duration.ofSeconds(30))
    .responseTimeout(Duration.ofSeconds(45))

  /**
   * get new DockerClient instance
   */
  def newDockerClient(): DockerClient = {
    DockerClientImpl.getInstance(dockerClientConf, dockerHttpClientBuilder.build())
  }


}
