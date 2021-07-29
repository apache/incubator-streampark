/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.codebuild

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.util.Logger
import org.apache.maven.plugins.shade.{DefaultShader, ShadeRequest}
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.codehaus.plexus.logging.{Logger => PlexusLogger}
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.{ArtifactDescriptorRequest, ArtifactRequest}

import java.io.File
import java.util
import javax.annotation.{Nonnull, Nullable}
import scala.collection.JavaConverters._


/**
 * author: Al-assad
 */
object MavenTool extends Logger {

  val plexusLog = new ConsoleLogger(PlexusLogger.LEVEL_INFO, "streamx-maven")

  private val isJarFile = (file: File) => file.isFile && file.getName.endsWith(".jar")


  /**
   * Build fat-jar with custom jar libs
   *
   * @param jarLibs       list of jar lib paths for building fat-jar
   * @param outfatJarPath output paths of fat-jar, like "/streamx/workspace/233/my-fat.jar"
   * @return File Object of output fat-jar
   */
  @Nonnull
  def buildFatJar(@Nonnull jarLibs: Array[String], @Nonnull outfatJarPath: String): File = {
    // check userJarPath
    val uberJar = new File(outfatJarPath)
    if (uberJar.isDirectory) {
      throw new Exception(s"[Streamx-Maven] outfatJarPath(${outfatJarPath}) should be a file.")
    }
    // resolve all jarLibs
    val jarSet = new util.HashSet[File]
    jarLibs.map(lib => new File(lib))
      .filter(lib => lib.exists())
      .distinct
      .foreach(lib =>
        if (isJarFile(lib)) {
          jarSet.add(lib)
        } else if (lib.isDirectory) {
          lib.listFiles.filter(isJarFile).foreach(jar => jarSet.add(jar))
        }
      )
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
   * Build fat-jar with custom jar libs and maven artifacts
   *
   * @param jarLibs        list of jar lib paths
   * @param mavenArtifacts collection of maven artifacts
   * @param outfatJarPath  output paths of fat-jar
   * @return File Object of output fat-jar
   */
  @Nonnull
  def buildFatJar(@Nullable jarLibs: Array[String], @Nullable mavenArtifacts: Array[MavenArtifact],
                  @Nonnull outfatJarPath: String): File = {
    val libs = if (jarLibs == null) Array[String]() else jarLibs
    val arts = if (mavenArtifacts == null) Array[MavenArtifact]() else mavenArtifacts
    if (libs.isEmpty && arts.isEmpty) {
      throw new Exception(s"[Streamx-Maven] empty artifacts.")
    }
    val artFilePaths = resolveArtifacts(arts).map(file => file.getAbsolutePath)
    buildFatJar(libs ++ artFilePaths, outfatJarPath)
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
  def resolveArtifacts(mavenArtifacts: Array[MavenArtifact]): Array[File] = {
    if (mavenArtifacts == null) {
      return Array[File]()
    }
    val (repoSystem, session) = MavenRetriever.retrieve()
    val artifacts = mavenArtifacts.map(e => new DefaultArtifact(e.groupId, e.artifactId, "jar", e.version)).toSet
    logInfo(s"start resolving dependencies: ${artifacts.mkString}")

    // read relevant artifact descriptor info
    val resolvedArtifacts = artifacts
      .map(art => new ArtifactDescriptorRequest(art, MavenRetriever.remoteRepos, null))
      .map(artReq => repoSystem.readArtifactDescriptor(session, artReq))
      .flatMap(artRes => artRes.getDependencies.asScala)
      .filter(dependency => "compile".equals(dependency.getScope))
      .map(dependency => dependency.getArtifact)
    logInfo(s"resolved dependencies: ${resolvedArtifacts.mkString}")

    // download artifacts
    val artReqs = resolvedArtifacts.map(art => new ArtifactRequest(art, MavenRetriever.remoteRepos, null)).asJava
    repoSystem
      .resolveArtifacts(session, artReqs)
      .asScala
      .map(artRes => artRes.getArtifact.getFile)
      .toArray
  }


}
