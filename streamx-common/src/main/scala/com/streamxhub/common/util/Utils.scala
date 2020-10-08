package com.streamxhub.common.util

import java.io.{File, IOException}
import java.net.URL
import java.util.UUID
import java.util.jar.JarFile
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.util.jar.JarInputStream
import scala.util.{Failure, Success, Try}

object Utils {

  def uuid(): String = UUID.randomUUID().toString.replaceAll("-", "")

  @throws[IOException] def checkJarFile(jar: URL): Unit = {
    val jarFile: File = Try(new File(jar.toURI)) match {
      case Success(x) => x
      case Failure(_) => throw new IOException(s"JAR file path is invalid $jar")
    }
    if (!jarFile.exists) {
      throw new IOException(s"JAR file does not exist '${jarFile.getAbsolutePath}'")
    }
    if (!jarFile.canRead) {
      throw new IOException(s"JAR file can't be read '${jarFile.getAbsolutePath}'")
    }
    Try(new JarFile(jarFile)) match {
      case Failure(e) => throw new IOException(s"Error while opening jar file '${jarFile.getAbsolutePath}'", e)
      case Success(x) => x.close()
    }
  }

  def getJarManifest(jarFile: File) = {
    checkJarFile(jarFile.toURL)
    new JarInputStream(new BufferedInputStream(new FileInputStream(jarFile))).getManifest
  }

  def main(args: Array[String]): Unit = {
    val jar = "/Users/benjobs/Workspace/deploy/workspace/app/1/flink-quickstart-1.0/flink-quickstart-1.0.jar"
    val manifest = getJarManifest(new File(jar))
    println(manifest.getMainAttributes.getValue("Main-Class"))
  }

}
