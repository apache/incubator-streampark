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

package org.apache.streampark.flink.kubernetes;

public final class FlinkK8sWatcherFactory {

  /**
   * Create FlinkK8sWatcher instance.
   *
   * @param conf configuration
   * @param lazyStart Whether monitor will perform delayed auto-start when necessary. In this case,
   *     there is no need to display the call to FlinkK8sWatcher.start(), useless the monitor is
   *     expected to start immediately.
   */
  public static FlinkK8sWatcher createInstance(
      TrackConfig.FlinkTrackConfig conf, boolean lazyStart) {
    if (lazyStart) {
      // TODO: Implement lazy start logic
      return new DefaultFlinkK8sWatcher(conf);
    } else {
      return new DefaultFlinkK8sWatcher(conf);
    }
  }
}
