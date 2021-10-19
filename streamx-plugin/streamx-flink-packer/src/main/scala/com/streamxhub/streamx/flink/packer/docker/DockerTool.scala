package com.streamxhub.streamx.flink.packer.docker

import com.github.dockerjava.api.command.PushImageCmd
import com.github.dockerjava.api.exception.InternalServerErrorException
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
        // pull docker image
        val pullImageCmd = dockerClient.pullImageCmd(dockerFileTemplate.flinkBaseImage).withAuthConfig(authConf.toDockerAuthConf)
        pullImageCmd.start().awaitCompletion()
        logInfo(s"[streamx-packer] docker pull image ${dockerFileTemplate.flinkBaseImage} successfully.")
        // build docker image
        val buildImageCmd = dockerClient.buildImageCmd()
          .withBaseDirectory(projectDir)
          .withDockerfile(dockerfile)
          .withTags(Sets.newHashSet(tagName))
        val imageId = buildImageCmd.start().awaitImageId()
        logInfo(s"[streamx-packer] docker image built successfully, imageId=${imageId}, tag=${tagName}")
        // push docker image
        if (push) {
          val pushCmd: PushImageCmd = dockerClient.pushImageCmd(tagName).withAuthConfig(authConf.toDockerAuthConf)
          pushCmd.start().awaitCompletion()
          logInfo(s"[streamx-packer] docker image push successfully, tag=${tagName}, registerAddr=${authConf.registerAddress}")
        }
    } {
      case cause: InternalServerErrorException =>
        logError(s"[streamx] pull flink base docker image failed, imageTag=${dockerFileTemplate.flinkBaseImage}", cause)
        throw new Exception(s"[streamx] pull flink base docker image failed, imageTag=${dockerFileTemplate.flinkBaseImage}", cause)
      case cause =>
        logError("[streamx-packer] build and push flink job docker image failed.", cause)
        throw new Exception("[streamx-packer] build and push flink job docker image failed.", cause)
    }
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
        logError(s"[streamx-packer] push docker image fail, tag=$imageTag, registerAddress=${authConf.registerAddress}," +
          s" exception=${exception.getMessage}")
        false
    }
  }


  /**
   * compile image tag with namespace and remote address.
   */
  private def compileTag(tag: String, registerAddress: String): String = {
    var tagName = if (tag.contains("/")) tag else s"$DOCKER_IMAGE_NAMESPACE/$tag"
    if (registerAddress.nonEmpty && !tagName.startsWith(registerAddress))
      tagName = s"$registerAddress/$tagName"
    tagName
  }


}
