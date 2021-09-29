package com.streamxhub.streamx.flink.packer.docker

import com.github.dockerjava.api.command.PushImageCmd
import com.google.common.collect.Sets
import com.streamxhub.streamx.common.conf.ConfigConst.DOCKER_IMAGE_NAMESPACE
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.common.util.Utils.tryWithResourceException
import org.apache.commons.io.FileUtils

import java.io.File

/**
 * author: Al-assad
 */
//noinspection DuplicatedCode
object DockerTool extends Logger {


  /**
   * build and push docker image for flink fat-jar.
   * author: Al-assad
   *
   * @param authConf           authentication configuration of remote docker register
   * @param projectBaseDir     project workspace dir of flink job
   * @param dockerFileTemplate flink jar docker build template
   * @param expectImageTag     expect image tag for fat-jar of flink job
   * @param push               whether push image after build image
   * @return actual flink job jar image name
   */
  @throws[Exception] def buildFlinkImage(authConf: DockerAuthConf,
                                         projectBaseDir: String,
                                         dockerFileTemplate: FlinkDockerfileTemplate,
                                         expectImageTag: String,
                                         push: Boolean = false): String = {
    // organize project path and write docker file
    val projectDir = new File(projectBaseDir)
    if (!projectDir.exists()) {
      projectDir.mkdir()
    }
    val flinkFatJar = new File(dockerFileTemplate.flinkFatjarPath)
    if (flinkFatJar.getParentFile.getAbsolutePath != projectDir.getAbsolutePath) {
      FileUtils.copyFile(flinkFatJar, new File(s"${projectDir.getAbsolutePath}/${flinkFatJar.getName}"))
    }
    // generate dockerfile
    val dockerfile = dockerFileTemplate.writeDockerfile(projectBaseDir)
    val tagName = compileTag(expectImageTag, authConf.registerAddress)

    // build and push docker image
    tryWithResourceException(DockerRetriever.newDockerClient()) {
      dockerClient =>
        // build docker image
        val buildImageCmd = dockerClient.buildImageCmd()
          .withPull(true)
          .withBaseDirectory(projectDir)
          .withDockerfile(dockerfile)
          .withTags(Sets.newHashSet(tagName))
        buildImageCmd.start().awaitCompletion()
        logInfo(s"[streamx-packer] docker image built successfully, tag=${tagName}")
        // push docker image
        if (push) {
          val pushCmd: PushImageCmd = dockerClient.pushImageCmd(tagName).withAuthConfig(authConf.toDockerAuthConf)
          pushCmd.start().awaitCompletion()
          logInfo(s"[streamx-packer] docker image push successfully, tag=${tagName}, registerAddr=${authConf.registerAddress}")
        }
    }(
      exception => throw new Exception("[streamx-packer] build and push flink job docker image fail", exception)
    )
    tagName
  }


  /**
   * push docker image to remote regoster
   *
   * @return successful or failed
   */
  @throws[Exception] def pushImage(imageTag: String, authConf: DockerAuthConf): Boolean = {
    tryWithResourceException(DockerRetriever.newDockerClient()) {
      client =>
        val pushCmd: PushImageCmd = client.pushImageCmd(imageTag).withAuthConfig(authConf.toDockerAuthConf)
        pushCmd.start().awaitCompletion()
        true
    } {
      exception =>
        logError(s"[streamx-packer] push docker image fail, tag=${imageTag}, registerAddr=${authConf.registerAddress}," +
          s" exception=${exception.getMessage}")
        false
    }
  }


  /**
   * compile image tag with namespace and remote address.
   */
  private def compileTag(tag: String, registerAddr: String): String = {
    var tagName = if (tag.contains("/")) tag else s"$DOCKER_IMAGE_NAMESPACE/$tag"
    if (registerAddr.nonEmpty && !tagName.startsWith(registerAddr))
      tagName = s"$registerAddr/$tagName"
    tagName
  }


}
