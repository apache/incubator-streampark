package com.streamxhub.streamx.flink.packer.maven

import com.google.common.collect.Lists
import com.streamxhub.streamx.common.conf.ConfigConst.{DEFAULT_MAVEN_REMOTE_URL, MAVEN_LOCAL_DIR}
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
  lazy val localRepo = new LocalRepository(MAVEN_LOCAL_DIR)

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
