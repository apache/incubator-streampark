package com.streamxhub.common.util

import java.io.File
import java.net.URL
import java.lang.reflect.Method
import java.net.URLClassLoader

object ClassLoaderUtils {

  private val classloader = ClassLoader.getSystemClassLoader.asInstanceOf[URLClassLoader]

  def loadJar(jarFilePath: String): Unit = {
    val jarFile = new File(jarFilePath)
    require(jarFile.exists, s"[JobX] jarFilePath:$jarFilePath is not exists")
    require(jarFile.isFile, s"[JobX] jarFilePath:$jarFilePath is not file")
    loadPath(jarFile.getAbsolutePath)
  }

  def loadJars(path: String): Unit = {
    val jarDir = new File(path)
    require(jarDir.exists, s"[JobX] jarPath: $path is not exists")
    require(jarDir.isFile, s"[JobX] jarPath: $path is not directory")
    require(jarDir.listFiles.length > 0, s"[JobX] have not jar in path:$path")
    for (jarFile <- jarDir.listFiles) {
      loadPath(jarFile.getAbsolutePath)
    }
  }

  /**
   * URLClassLoader的addURL方法
   */
  private val addURL: Method = try {
    val add = classOf[URLClassLoader].getDeclaredMethod("addURL", Array(classOf[URL]): _*)
    add.setAccessible(true)
    add
  } catch {
    case e: Exception => throw new RuntimeException(e)
  }

  private def loadPath(filepath: String): Unit = {
    val file = new File(filepath)
    loopFiles(file)
  }

  private def loadResourceDir(filepath: String): Unit = {
    val file = new File(filepath)
    loopDirs(file)
  }

  private def loopDirs(file: File): Unit = { // 资源文件只加载路径
    if (file.isDirectory) {
      addURL(file)
      val tmps = file.listFiles
      for (tmp <- tmps) {
        loopDirs(tmp)
      }
    }
  }


  private def loopFiles(file: File): Unit = {
    if (file.isDirectory) {
      val tmps = file.listFiles
      for (tmp <- tmps) {
        loopFiles(tmp)
      }
    }
    else if (file.getAbsolutePath.endsWith(".jar") || file.getAbsolutePath.endsWith(".zip")) addURL(file)
  }

  private def addURL(file: File): Unit = {
    try {
      addURL.invoke(classloader, Array[AnyRef](file.toURI.toURL))
    } catch {
      case e: Exception =>
    }
  }


}
