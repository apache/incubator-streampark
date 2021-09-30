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
package com.streamxhub.streamx.flink.packer.maven

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.conf.ConfigConst.DEFAULT_MAVEN_REMOTE_URL
import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.enums.StorageType
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.repository.{LocalRepository, RemoteRepository}
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.{RepositorySystem, RepositorySystemSession}

import java.util

/**
 * author: Al-assad
 */
object MavenRetriever {

  /**
   * default maven remote center repository
   */
  lazy val remoteCenterRepo: RemoteRepository =
    new RemoteRepository.Builder("central", "default", DEFAULT_MAVEN_REMOTE_URL).build()

  /**
   * maven remote repository lists
   */
  lazy val remoteRepos: util.ArrayList[RemoteRepository] = Lists.newArrayList(remoteCenterRepo)

  /**
   * default maven local repository
   */
  lazy val localRepo = new LocalRepository(new Workspace(StorageType.LFS).MAVEN_LOCAL_DIR)

  private lazy val locator = MavenRepositorySystemUtils.newServiceLocator

  /**
   * create maven repository endpoint
   */
  def newRepoSystem(): RepositorySystem = {
    locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
    locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    locator.getService(classOf[RepositorySystem])
  }

  /**
   * create maven repository session endpoint
   */
  def newSession(system: RepositorySystem): RepositorySystemSession = {
    val session = MavenRepositorySystemUtils.newSession
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
    session
  }

  /**
   * create composite maven endpoint
   */
  def retrieve(): (RepositorySystem, RepositorySystemSession) = {
    val repoSystem = newRepoSystem()
    val session = newSession(repoSystem)
    (repoSystem, session)
  }


}
