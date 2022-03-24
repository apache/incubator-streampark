/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.packer.maven

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.util.{Logger, Utils}
import org.apache.maven.plugins.shade.resource.{ManifestResourceTransformer, ResourceTransformer, ServicesResourceTransformer}
import org.apache.maven.plugins.shade.{DefaultShader, ShadeRequest}
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.codehaus.plexus.logging.{Logger => PlexusLog}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.{ArtifactDescriptorRequest, ArtifactRequest}

import java.io.File
import java.util
import javax.annotation.{Nonnull, Nullable}
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * @author Al-assad
 */
object MavenTool extends Logger {

  val plexusLog = new ConsoleLogger(PlexusLog.LEVEL_INFO, "streamx-maven")

  private[this] val excludeArtifact = List(
    MavenArtifact.of("org.apache.flink:force-shading:*"),
    MavenArtifact.of("com.google.code.findbugs:jsr305:*"),
    MavenArtifact.of("org.apache.logging.log4j:*:*")
  )

  private val isJarFile = (file: File) => file.isFile && Try(Utils.checkJarFile(file.toURI.toURL)).isSuccess

  /**
   * Build a fat-jar with custom jar libraries.
   *
   * @param jarLibs       list of jar lib paths for building fat-jar
   * @param outFatJarPath output paths of fat-jar, like "/streamx/workspace/233/my-fat.jar"
   * @return File Object of output fat-jar
   */
  @throws[Exception] def buildFatJar(@Nullable mainClass: String, @Nonnull jarLibs: Set[String], @Nonnull outFatJarPath: String): File = {
    // check userJarPath
    val uberJar = new File(outFatJarPath)
    require(outFatJarPath.endsWith(".jar") && !uberJar.isDirectory, s"[StreamX] streamx-packer: outFatJarPath($outFatJarPath) should be a JAR file.")
    uberJar.delete()
    // resolve all jarLibs
    val jarSet = new util.HashSet[File]
    jarLibs.map(lib => new File(lib))
      .filter(_.exists)
      .foreach {
        case libFile if isJarFile(libFile) => jarSet.add(libFile)
        case libFile if libFile.isDirectory => libFile.listFiles.filter(isJarFile).foreach(jarSet.add)
        case _ =>
      }
    logInfo(s"start shaded fat-jar: ${jarLibs.mkString(",")}")
    // shade jars
    val shadeRequest = {
      val req = new ShadeRequest
      req.setJars(jarSet)
      req.setUberJar(uberJar)
      req.setFilters(Lists.newArrayList())

      val transformer = ArrayBuffer[ResourceTransformer]()
      // ref https://ci.apache.org/projects/flink/flink-docs-master/docs/connectors/table/overview/#transform-table-connectorformat-resources
      transformer += new ServicesResourceTransformer()
      if (mainClass != null) {
        val manifest = new ManifestResourceTransformer()
        manifest.setMainClass(mainClass)
        transformer += manifest
      }

      req.setResourceTransformers(transformer.toList)
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
   * @param dependencyInfo maven artifacts and jar libraries for building a fat-jar
   * @param outFatJarPath  output paths of fat-jar, like "/streamx/workspace/233/my-fat.jar"
   */
  @throws[Exception] def buildFatJar(@Nullable mainClass: String,
                                     @Nonnull dependencyInfo: DependencyInfo,
                                     @Nonnull outFatJarPath: String): File = {
    val jarLibs = dependencyInfo.extJarLibs
    val arts = dependencyInfo.mavenArts
    if (jarLibs.isEmpty && arts.isEmpty) {
      throw new Exception(s"[StreamX] streamx-packer: empty artifacts.")
    }
    val artFilePaths = resolveArtifacts(arts).map(_.getAbsolutePath)
    buildFatJar(mainClass, jarLibs ++ artFilePaths, outFatJarPath)
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
      logInfo(s"start resolving dependencies: ${artifacts.mkString}")

      // read relevant artifact descriptor info
      // plz don't simplify the following lambda syntax to maintain the readability of the code.
      val resolvedArtifacts = artifacts
        .map(artifact => new ArtifactDescriptorRequest(artifact, MavenRetriever.remoteRepos(), null))
        .map(artDescReq => repoSystem.readArtifactDescriptor(session, artDescReq))
        .flatMap(_.getDependencies)
        .filter(_.getScope == "compile")
        .filter(x => !excludeArtifact.exists(e => {
          val groupId = e.groupId == x.getArtifact.getGroupId
          val artifact = e.artifactId match {
            case "*" => true
            case a => a == x.getArtifact.getArtifactId
          }
          groupId && artifact
        })
        ).map(_.getArtifact)

      val mergedArtifacts = artifacts ++ resolvedArtifacts
      logInfo(s"resolved dependencies: ${mergedArtifacts.mkString}")

      // download artifacts
      val artReqs = mergedArtifacts.map(artifact => new ArtifactRequest(artifact, MavenRetriever.remoteRepos(), null))
      repoSystem.resolveArtifacts(session, artReqs)
        .map(_.getArtifact.getFile).toSet
    }
  }


}
