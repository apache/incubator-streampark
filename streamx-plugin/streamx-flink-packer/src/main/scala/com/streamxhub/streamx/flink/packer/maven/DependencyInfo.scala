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

import java.util.{List => JavaList}
import scala.collection.JavaConversions._

/**
 * @author Al-assad
 * @param mavenArts  collection of maven artifacts
 * @param extJarLibs collection of jar lib paths, which elements can be a directory or file path.
 */
case class DependencyInfo(mavenArts: Set[MavenArtifact] = Set(),
                          extJarLibs: Set[String] = Set()) {

  def this(mavenArts: JavaList[MavenArtifact], extJarLibs: JavaList[String]) {
    this(mavenArts.toSet, extJarLibs.toSet)
  }

  def merge(jarLibs: Set[String]): DependencyInfo =
    if (jarLibs != null) DependencyInfo(mavenArts, extJarLibs ++ jarLibs) else this.copy()

}

object DependencyInfo {
  def empty: DependencyInfo = new DependencyInfo()
}



