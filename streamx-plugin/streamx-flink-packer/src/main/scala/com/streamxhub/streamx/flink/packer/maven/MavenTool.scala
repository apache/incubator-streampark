package com.streamxhub.streamx.flink.packer.maven

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.util.{Logger, Utils}
import org.apache.maven.plugins.shade.{DefaultShader, ShadeRequest}
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.codehaus.plexus.logging.{Logger => PlexusLog}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.{ArtifactDescriptorRequest, ArtifactRequest}

import java.io.File
import java.util
import javax.annotation.Nonnull
import scala.collection.JavaConverters._
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
  @Nonnull
  def buildFatJar(@Nonnull jarLibs: Set[String], @Nonnull outFatJarPath: String): File = {
    // check userJarPath
    val uberJar = new File(outFatJarPath)
    if (uberJar.isDirectory) {
      throw new Exception(s"[Streamx-Maven] outFatJarPath($outFatJarPath) should be a file.")
    }
    // resolve all jarLibs
    val jarSet = new util.HashSet[File]
    jarLibs.map(lib => new File(lib))
      .filter(_.exists)
      .foreach {
        case libFile if isJarFile(libFile) => jarSet.add(libFile)
        case libFile if libFile.isDirectory => libFile.listFiles.filter(isJarFile).foreach(jarSet.add)
        case _ =>
      }
    logInfo(s"start shaded fat-jar: ${jarLibs.mkString}")
    // shade jars
    val shadeRequest = {
      val req = new ShadeRequest
      req.setJars(jarSet)
      req.setUberJar(uberJar)
      req.setFilters(Lists.newArrayList())
      req.setResourceTransformers(Lists.newArrayList())
      req.setRelocators(Lists.newArrayList())
      req
    }
    val shader = new DefaultShader()
    shader.enableLogging(plexusLog)
    shader.shade(shadeRequest)
    logInfo(s"finish build fat-jar: ${uberJar.getAbsolutePath}")
    uberJar
  }

  /**
   * Build a fat-jar with custom jar librarties and maven artifacts.
   *
   * @param jarPackDeps   maven artifacts and jar libraries for building a fat-jar
   * @param outFatJarPath output paths of fat-jar, like "/streamx/workspace/233/my-fat.jar"
   */
  def buildFatJar(@Nonnull jarPackDeps: JarPackDeps, @Nonnull outFatJarPath: String): File = {
    val jarlibs = jarPackDeps.extJarLibs
    val arts = jarPackDeps.mavenArts
    if (jarlibs.isEmpty && arts.isEmpty) {
      throw new Exception(s"[Streamx-Maven] empty artifacts.")
    }
    val artFilePaths = resolveArtifacts(arts).map(_.getAbsolutePath)
    buildFatJar(jarlibs ++ artFilePaths, outFatJarPath)
  }


  /**
   * Resolve the collectoin of artifacts, Artifacts will be download to
   * ConfigConst.MAVEN_LOCAL_DIR if necessary. notes: Only compile scope
   * dependencies will be resolved.
   *
   * @param mavenArtifacts collection of maven artifacts
   * @return jar File Object of resolved artifacts
   */
  @Nonnull
  def resolveArtifacts(mavenArtifacts: Set[MavenArtifact]): Set[File] = {
    if (mavenArtifacts == null) Set.empty[File]; else {
      val (repoSystem, session) = MavenRetriever.retrieve()
      val artifacts = mavenArtifacts.map(e => new DefaultArtifact(e.groupId, e.artifactId, "jar", e.version))
      logInfo(s"start resolving dependencies: ${artifacts.mkString}")

      // read relevant artifact descriptor info
      // plz don't simplify the following lambda syntax to maintain the readability of the code.
      val resolvedArtifacts = artifacts
        .map(artifact => new ArtifactDescriptorRequest(artifact, MavenRetriever.remoteRepos, null))
        .map(artDescReq => repoSystem.readArtifactDescriptor(session, artDescReq))
        .flatMap(artDescReq => artDescReq.getDependencies.asScala)
        .filter(dependency => dependency.getScope == "compile")
        .map(dependency => dependency.getArtifact)
      logInfo(s"resolved dependencies: ${resolvedArtifacts.mkString}")

      // download artifacts
      val artReqs = resolvedArtifacts.map(artifact => new ArtifactRequest(artifact, MavenRetriever.remoteRepos, null)).asJava
      repoSystem.resolveArtifacts(session, artReqs).asScala
        .map(artifactResult => artifactResult.getArtifact.getFile).toSet
    }
  }


}
