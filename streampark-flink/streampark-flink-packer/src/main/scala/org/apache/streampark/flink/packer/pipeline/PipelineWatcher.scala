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

package org.apache.streampark.flink.packer.pipeline

/** Trait for watching a BuildPipeline instance */
trait PipeWatcher {

  /** called when the pipeline is launched. */
  @throws[Exception]
  def onStart(snapshot: PipelineSnapshot): Unit

  /** called when the any status of building step is changed. */
  @throws[Exception]
  def onStepStateChange(snapshot: PipelineSnapshot): Unit

  /**
   * called when the pipeline is finished, or you can get the results directly from the
   * BuildPipeline.launch() synchronously.
   */
  @throws[Exception]
  def onFinish(snapshot: PipelineSnapshot, result: BuildResult): Unit
}

class SilentPipeWatcher extends PipeWatcher {

  override def onStart(snapshot: PipelineSnapshot): Unit = {}

  override def onStepStateChange(snapshot: PipelineSnapshot): Unit = {}

  override def onFinish(snapshot: PipelineSnapshot, result: BuildResult): Unit = {}
}
