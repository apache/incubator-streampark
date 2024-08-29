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

package org.apache.streampark.flink.core

import collection.JavaConverters._
import collection.mutable._
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.hadoop.fs.Path

import java.io.File
import java.util

class YarnClusterDescriptorWrapper(yarnClusterDescriptor: YarnClusterDescriptor)
  extends YarnClusterDescriptorTrait(yarnClusterDescriptor) {

  override def addShipFiles(input: util.List[File]) = {
    val shipFiles: Seq[File] = input.asScala
    var paths: ArrayBuffer[Path] = ArrayBuffer[Path]()
    for (item <- shipFiles) {
      val path = new Path(item.toURI)
      paths += path
    }
    yarnClusterDescriptor.addShipFiles(paths.asJava)
  }
}
