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
package org.apache.streampark.flink.kubernetes.v2

import org.apache.streampark.common.conf.InternalConfigHolder
import org.apache.streampark.flink.kubernetes.v2.FlinkK8sConfig.EMBEDDED_HTTP_FILE_SERVER_LOCAL_MIRROR_DIR
import org.apache.streampark.flink.kubernetes.v2.example.clearTestAssets

package object example {

  val testPath   = os.pwd / "streampark_workspace"
  val mirrorPath = testPath / "mirror"
  val assetPath  = testPath / "assets"

  def prepareTestAssets() = {
    // prepare test assets and embedded http file server local directory
    if (!os.exists(testPath)) os.makeDir(testPath)
    if (!os.exists(mirrorPath)) os.makeDir(mirrorPath)
    if (os.exists(assetPath)) os.remove.all(assetPath)
    os.copy(os.Path(getClass.getResource("/assets").getPath), assetPath)

    // force set streampark system configuration
    InternalConfigHolder.set(EMBEDDED_HTTP_FILE_SERVER_LOCAL_MIRROR_DIR, mirrorPath.toString)
  }

  def clearTestAssets() = {
    os.remove.all(testPath)
  }
}

/** Clear file resources during testing. */
object CleanTestResource extends App {
  clearTestAssets()
}
