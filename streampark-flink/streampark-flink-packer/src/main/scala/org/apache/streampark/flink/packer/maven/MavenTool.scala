/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.packer.maven

import org.apache.streampark.common.conf.{InternalConfigHolder, Workspace}
import org.apache.streampark.common.conf.CommonConfig.{MAVEN_AUTH_PASSWORD, MAVEN_AUTH_USER, MAVEN_REMOTE_URL}
import org.apache.streampark.common.constants.Constants
import org.apache.streampark.common.util.{AssertUtils, Logger, Utils}
import org.apache.streampark.common.util.Implicits._

import com.google.common.collect.Lists
import org.apache.maven.plugins.shade.{DefaultShader, ShadeRequest}
import org.apache.maven.plugins.shade.filter.Filter
import org.apache.maven.plugins.shade.resource.{ManifestResourceTransformer, ResourceTransformer, ServicesResourceTransformer}
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.codehaus.plexus.logging.{Logger => PlexusLog}
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.eclipse.aether.{RepositorySystem, RepositorySystemSession}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.repository.{LocalRepository, RemoteRepository}
import org.eclipse.aether.resolution.{ArtifactDescriptorRequest, ArtifactRequest}
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder

import javax.annotation.{Nonnull, Nullable}

import java.io.File
import java.util

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object MavenTool extends Logger {

  private[this] lazy val plexusLog =
    new ConsoleLogger(PlexusLog.LEVEL_INFO, "streampark-maven")

  private[this] val excludeArtifact = List(
    Artifact.of("org.apache.flink:force-shading:*"),
    Artifact.of("com.google.code.findbugs:jsr305:*"),
    Artifact.of("org.apache.logging.log4j:*:*"))

  private[this] def getRemoteRepos(): List[RemoteRepository] = {
    val builder =
      new RemoteRepository.Builder(
        "central",
        Constants.DEFAULT,
        InternalConfigHolder.get(MAVEN_REMOTE_URL))
    val remoteRepository = {
      val buildState =
        InternalConfigHolder.get(MAVEN_AUTH_USER) == null || InternalConfigHolder.get(
          MAVEN_AUTH_PASSWORD) == null
      if (buildState) {
        builder.build()
      } else {
        val authentication = new AuthenticationBuilder()
          .addUsername(InternalConfigHolder.get[String](MAVEN_AUTH_USER))
          .addPassword(InternalConfigHolder.get[String](MAVEN_AUTH_PASSWORD))
          .build()
        builder.setAuthentication(authentication).build()
      }
    }
    List(remoteRepository)
  }

  private val isJarFile = (file: File) =>
    file.isFile && Try(Utils.requireCheckJarFile(file.toURI.toURL)).isSuccess

  /**
   * Build a fat-jar with custom jar libraries.
   *
   * @param jarLibs
   *   list of jar lib paths for building fat-jar
   * @param outFatJarPath
   *   output paths of fat-jar, like "/streampark/workspace/233/my-fat.jar"
   * @return
   *   File Object of output fat-jar
   */
  @throws[Exception]
  def buildFatJar(
      @Nullable mainClass: String,
      @Nonnull jarLibs: Set[String],
      @Nonnull outFatJarPath: String): File = {
    // check userJarPath
    val uberJar = new File(outFatJarPath)
    require(
      outFatJarPath.endsWith(Constants.JAR_SUFFIX) && !uberJar.isDirectory,
      s"[StreamPark] streampark-packer: outFatJarPath($outFatJarPath) should be a JAR file.")
    uberJar.delete()
    // resolve all jarLibs
    val jarSet = new util.HashSet[File]
    jarLibs
      .map(lib => new File(lib))
      .filter(_.exists)
      .foreach {
        case libFile if isJarFile(libFile) => jarSet.add(libFile)
        case libFile if libFile.isDirectory =>
          libFile.listFiles.filter(isJarFile).foreach(jarSet.add)
        case _ =>
      }
    logInfo(s"start shaded fat-jar: ${jarLibs.mkString(",")}")
    // shade jars
    val shadeRequest = {
      val req = new ShadeRequest
      req.setJars(jarSet)
      req.setUberJar(uberJar)

      val transformer = ArrayBuffer[ResourceTransformer]()
      // ref https://ci.apache.org/projects/flink/flink-docs-master/docs/connectors/table/overview/#transform-table-connectorformat-resources
      transformer += new ServicesResourceTransformer()
      if (mainClass != null) {
        val manifest = new ManifestResourceTransformer()
        manifest.setMainClass(mainClass)
        transformer += manifest
      }
      req.setResourceTransformers(transformer.toList)
      req.setFilters(List(new ShadeFilter))
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
   * @param dependencyInfo
   *   maven artifacts and jar libraries for building a fat-jar
   * @param outFatJarPath
   *   output paths of fat-jar, like "/streampark/workspace/233/my-fat.jar"
   */
  @throws[Exception]
  def buildFatJar(
      @Nullable mainClass: String,
      @Nonnull dependencyInfo: DependencyInfo,
      @Nonnull outFatJarPath: String): File = {
    val jarLibs = dependencyInfo.extJarLibs
    val arts = dependencyInfo.mavenArts
    AssertUtils.required(
      !(jarLibs.isEmpty && arts.isEmpty),
      s"[StreamPark] streampark-packer: empty artifacts.")

    val artFilePaths = resolveArtifacts(arts).map(_.getAbsolutePath)
    buildFatJar(mainClass, jarLibs ++ artFilePaths, outFatJarPath)
  }

  @throws[Exception]
  def resolveArtifacts(mavenArtifact: Artifact): JavaList[File] =
    resolveArtifacts(Set(mavenArtifact))

  /**
   * Resolve the collectoin of artifacts, Artifacts will be download to ConfigConst.MAVEN_LOCAL_DIR
   * if necessary. notes: Only compile scope dependencies will be resolved.
   *
   * @param mavenArtifacts
   *   collection of maven artifacts
   * @return
   *   jar File Object of resolved artifacts
   */
  @throws[Exception]
  def resolveArtifacts(mavenArtifacts: JavaSet[Artifact]): JavaList[File] = {
    if (mavenArtifacts == null) List.empty[File]
    else {
      val (repoSystem, session) = getMavenEndpoint()
      val artifacts = mavenArtifacts.map(e => {
        val artifact =
          new DefaultArtifact(e.groupId, e.artifactId, e.classifier, "jar", e.version)
        artifact.getProperties
        artifact
      })
      logInfo(s"start resolving dependencies: ${artifacts.mkString}")

      val remoteRepos = getRemoteRepos()
      // read relevant artifact descriptor info
      // plz don't simplify the following lambda syntax to maintain the readability of the code.
      val resolvedArtifacts = artifacts
        .map(artifact => new ArtifactDescriptorRequest(artifact, remoteRepos, null))
        .map(artDescReq => repoSystem.readArtifactDescriptor(session, artDescReq))
        .flatMap(_.getDependencies)
        .filter(_.getScope == "compile")
        .filter(x => !excludeArtifact.exists(_.eq(x.getArtifact)))
        .map(_.getArtifact)

      val mergedArtifacts = artifacts ++ resolvedArtifacts
      logInfo(s"resolved dependencies: ${mergedArtifacts.mkString}")

      // download artifacts
      val artReqs =
        mergedArtifacts.map(artifact => new ArtifactRequest(artifact, remoteRepos, null))
      repoSystem
        .resolveArtifacts(session, artReqs)
        .map(_.getArtifact.getFile)
        .toList
    }
  }

  /** create composite maven endpoint */
  private[this] def getMavenEndpoint(): (RepositorySystem, RepositorySystemSession) = {

    /** create maven repository endpoint */

    lazy val locator = MavenRepositorySystemUtils.newServiceLocator

    /** default maven local repository */
    lazy val localRepo = new LocalRepository(Workspace.MAVEN_LOCAL_PATH)

    def newRepoSystem(): RepositorySystem = {
      locator.addService(
        classOf[RepositoryConnectorFactory],
        classOf[BasicRepositoryConnectorFactory])
      locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
      locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
      locator.getService(classOf[RepositorySystem])
    }

    /** create maven repository session endpoint */
    def newSession(system: RepositorySystem): RepositorySystemSession = {
      val session = MavenRepositorySystemUtils.newSession
      session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
      session
    }

    val repoSystem = newRepoSystem()
    val session = newSession(repoSystem)
    (repoSystem, session)
  }

  private[this] class ShadeFilter extends Filter {
    override def canFilter(jar: File): Boolean = true

    override def isFiltered(name: String): Boolean = {
      val isFilteredState =
        name.startsWith("META-INF/") && name.endsWith(".SF") || name.endsWith(".DSA") || name
          .endsWith(".RSA")
      if (isFilteredState) {
        logInfo(s"shade ignore file: $name")
        return true
      }
      false
    }

    override def finished(): Unit = {}
  }

}
