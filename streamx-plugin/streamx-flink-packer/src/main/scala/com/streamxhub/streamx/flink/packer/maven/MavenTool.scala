package com.streamxhub.streamx.flink.packer.maven

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.util.{Logger, Utils}
import org.apache.maven.plugins.shade.resource.ServicesResourceTransformer
import org.apache.maven.plugins.shade.{DefaultShader, ShadeRequest}
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.codehaus.plexus.logging.{Logger => PlexusLog}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.{ArtifactDescriptorRequest, ArtifactRequest}

import java.io.File
import java.util
import javax.annotation.Nonnull
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * author: Al-assad
 */
object MavenTool extends Logger {

  val plexusLog = new ConsoleLogger(PlexusLog.LEVEL_INFO, "streamx-maven")

  private val isJarFile = (file: File) => file.isFile && Try(Utils.checkJarFile(file.toURL)).isSuccess

  /**
   * Build a fat-jar with custom jar libraries.
   *
   * @param jarLibs       list of jar lib paths for building fat-jar
   * @param outFatJarPath output paths of fat-jar, like "/streamx/workspace/233/my-fat.jar"
   * @return File Object of output fat-jar
   */
  @throws[Exception] def buildFatJar(@Nonnull jarLibs: Set[String], @Nonnull outFatJarPath: String): File = {
    // check userJarPath
    val uberJar = new File(outFatJarPath)
    require(outFatJarPath.endsWith(".jar") && !uberJar.isDirectory, s"[streamx-packer] outFatJarPath($outFatJarPath) should be a JAR file.")
    // resolve all jarLibs
    val jarSet = new util.HashSet[File]
    jarLibs.map(lib => new File(lib))
      .filter(_.exists)
      .foreach {
        case libFile if isJarFile(libFile) => jarSet.add(libFile)
        case libFile if libFile.isDirectory => libFile.listFiles.filter(isJarFile).foreach(jarSet.add)
        case _ =>
      }
    logInfo(s"[streamx-packer] start shaded fat-jar: ${jarLibs.mkString(",")}")
    // shade jars
    val shadeRequest = {
      val req = new ShadeRequest
      req.setJars(jarSet)
      req.setUberJar(uberJar)
      req.setFilters(Lists.newArrayList())
      // ref https://ci.apache.org/projects/flink/flink-docs-master/docs/connectors/table/overview/#transform-table-connectorformat-resources
      req.setResourceTransformers(Lists.newArrayList(new ServicesResourceTransformer()))
      req.setRelocators(Lists.newArrayList())
      req
    }
    val shader = new DefaultShader()
    shader.enableLogging(plexusLog)
    shader.shade(shadeRequest)
    logInfo(s"[streamx-packer] finish build fat-jar: ${uberJar.getAbsolutePath}")
    uberJar
  }

  /**
   * Build a fat-jar with custom jar librarties and maven artifacts.
   *
   * @param jarPackDeps   maven artifacts and jar libraries for building a fat-jar
   * @param outFatJarPath output paths of fat-jar, like "/streamx/workspace/233/my-fat.jar"
   */
  @throws[Exception] def buildFatJar(@Nonnull jarPackDeps: JarPackDeps, @Nonnull outFatJarPath: String): File = {
    val jarLibs = jarPackDeps.extJarLibs
    val arts = jarPackDeps.mavenArts
    if (jarLibs.isEmpty && arts.isEmpty) {
      throw new Exception(s"[streamx-packer] empty artifacts.")
    }
    val artFilePaths = resolveArtifacts(arts).map(_.getAbsolutePath)
    buildFatJar(jarLibs ++ artFilePaths, outFatJarPath)
  }


  /**
   * Resolve the collectoin of artifacts, Artifacts will be download to
   * ConfigConst.MAVEN_LOCAL_DIR if necessary. notes: Only compile scope
   * dependencies will be resolved.
   *
   * @param mavenArtifacts collection of maven artifacts
   * @return jar File Object of resolved artifacts
   */
  @throws[Exception] def resolveArtifacts(mavenArtifacts: Set[MavenArtifact]): Set[File] = {
    if (mavenArtifacts == null) Set.empty[File]; else {
      val (repoSystem, session) = MavenRetriever.retrieve()
      val artifacts = mavenArtifacts.map(e => new DefaultArtifact(e.groupId, e.artifactId, "jar", e.version))
      logInfo(s"[streamx-packer] start resolving dependencies: ${artifacts.mkString}")

      // read relevant artifact descriptor info
      // plz don't simplify the following lambda syntax to maintain the readability of the code.
      val resolvedArtifacts = artifacts
        .map(artifact => new ArtifactDescriptorRequest(artifact, MavenRetriever.remoteRepos, null))
        .map(artDescReq => repoSystem.readArtifactDescriptor(session, artDescReq))
        .flatMap(_.getDependencies)
        .filter(_.getScope == "compile")
        .map(_.getArtifact)

      val mergedArtifacts = artifacts ++ resolvedArtifacts
      logInfo(s"[streamx-packer] resolved dependencies: ${mergedArtifacts.mkString}")

      // download artifacts
      val artReqs = mergedArtifacts.map(artifact => new ArtifactRequest(artifact, MavenRetriever.remoteRepos, null))
      repoSystem.resolveArtifacts(session, artReqs)
        .map(_.getArtifact.getFile).toSet
    }
  }


}
