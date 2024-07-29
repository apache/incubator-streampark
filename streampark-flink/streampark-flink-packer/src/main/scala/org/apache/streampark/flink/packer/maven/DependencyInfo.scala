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

import org.apache.streampark.common.util.Implicits._

/**
 * @param mavenArts
 *   collection of maven artifacts
 * @param extJarLibs
 *   collection of jar lib paths, which elements can be a directory or file path.
 */
case class DependencyInfo(mavenArts: Set[Artifact] = Set(), extJarLibs: Set[String] = Set()) {

  def this(mavenArts: JavaList[Artifact], extJarLibs: JavaList[String]) {
    this(mavenArts.toSet, extJarLibs.toSet)
  }

  def merge(jarLibs: Set[String]): DependencyInfo =
    if (jarLibs != null) {
      DependencyInfo(mavenArts, extJarLibs ++ jarLibs)
    } else {
      this.copy()
    }

  def merge(mvnPoms: JavaList[Artifact], jarLibs: JavaList[String]): DependencyInfo =
    if (mvnPoms != null && jarLibs != null) {
      DependencyInfo(mavenArts ++ mvnPoms.toSet, extJarLibs ++ jarLibs.toSet)
    } else if (mvnPoms != null) {
      DependencyInfo(mavenArts ++ mvnPoms.toSet, extJarLibs)
    } else if (jarLibs != null) {
      DependencyInfo(mavenArts, extJarLibs ++ jarLibs.toSet)
    } else {
      this.copy()
    }
}

object DependencyInfo {
  def empty: DependencyInfo = new DependencyInfo()
}
