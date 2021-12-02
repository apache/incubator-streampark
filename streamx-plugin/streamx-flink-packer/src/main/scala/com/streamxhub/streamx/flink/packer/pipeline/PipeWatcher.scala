/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.packer.pipeline

/**
 * Trait for watching a BuildPipeline instance
 *
 * @author Al-assad
 */
trait PipeWatcher {

  /**
   * called when the pipeline is launched.
   */
  def onStart(snapshot: PipeSnapshot): Unit

  /**
   * called when the any status of building step is changed.
   */
  def onStepStateChange(snapshot: PipeSnapshot): Unit

  /**
   * called when the pipeline is finished, or you can get the
   * results directly from the BuildPipeline.launch() synchronously.
   */
  def onFinish(snapshot: PipeSnapshot, result: BuildResult): Unit
}

/**
 * silent watcher
 */
class SilentPipeWatcher extends PipeWatcher {

  override def onStart(snapshot: PipeSnapshot): Unit = {}

  override def onStepStateChange(snapshot: PipeSnapshot): Unit = {}

  override def onFinish(snapshot: PipeSnapshot, result: BuildResult): Unit = {}

}
