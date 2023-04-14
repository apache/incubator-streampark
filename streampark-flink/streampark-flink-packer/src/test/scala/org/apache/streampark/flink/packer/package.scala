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

package org.apache.streampark.flink

import org.codehaus.plexus.util.IOUtil

import java.io.{InputStream, IOException}
import java.util
import java.util.jar.JarFile

package object packer {

  def path: String => String = (path: String) => getClass.getClassLoader.getResource(path).getFile

  def jarEquals(jar1: JarFile, jar2: JarFile, entry: String): Boolean = {
    var s1: InputStream = null
    var s2: InputStream = null
    try {
      s1 = jar1.getInputStream(jar1.getJarEntry(entry))
      s2 = jar2.getInputStream(jar2.getJarEntry(entry))
      util.Arrays.equals(IOUtil.toByteArray(s1), IOUtil.toByteArray(s2))
    } catch {
      case e: IOException => false
    } finally {
      if (s1 != null) s1.close()
      if (s2 != null) s2.close()
    }
  }

}
