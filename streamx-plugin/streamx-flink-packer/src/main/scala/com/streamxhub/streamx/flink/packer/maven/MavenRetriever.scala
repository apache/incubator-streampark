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
import com.streamxhub.streamx.common.conf.CommonConfig.{MAVEN_AUTH_PASSWORD, MAVEN_AUTH_USER, MAVEN_REMOTE_URL}
import com.streamxhub.streamx.common.conf.{InternalConfigHolder, Workspace}
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.repository.{LocalRepository, RemoteRepository}
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.eclipse.aether.{RepositorySystem, RepositorySystemSession}

import java.util

/**
 * @author Al-assad
 */
object MavenRetriever {

  /**
   * default maven local repository
   */
  lazy val localRepo = new LocalRepository(Workspace.local.MAVEN_LOCAL_DIR)

  private lazy val locator = MavenRepositorySystemUtils.newServiceLocator

  /**
   * maven remote repository lists
   */
  def remoteRepos(): util.ArrayList[RemoteRepository] = {
    val builder = new RemoteRepository.Builder("central", "default", InternalConfigHolder.get(MAVEN_REMOTE_URL))
    val remoteRepository = if (InternalConfigHolder.get(MAVEN_AUTH_USER) == null || InternalConfigHolder.get(MAVEN_AUTH_PASSWORD) == null) {
      builder.build()
    } else {
      val authentication = new AuthenticationBuilder()
        .addUsername(InternalConfigHolder.get[String](MAVEN_AUTH_USER))
        .addPassword(InternalConfigHolder.get[String](MAVEN_AUTH_PASSWORD))
        .build()
      builder.setAuthentication(authentication).build()
    }
    Lists.newArrayList(remoteRepository)
  }

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
