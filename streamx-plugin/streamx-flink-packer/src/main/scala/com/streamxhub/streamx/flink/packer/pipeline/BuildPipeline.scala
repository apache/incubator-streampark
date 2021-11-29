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

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.packer.pipeline.PipeStatus.PipeStatus
import com.streamxhub.streamx.flink.packer.pipeline.StepStatus.StepStatus

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
 * Behavior that BuildPipeline subclasses must inherit to implement.
 *
 * @author Al-assad
 */
trait BuildPipelineProcess {

  /**
   * the type of pipeline
   */
  val pipeType: PipeType

  /**
   * the actual build process.
   * the effective steps progress should be implemented in
   * multiple BuildPipeline.execStep() functions.
   */
  @throws[Throwable] protected def buildProcess(): BuildResult

  /**
   * the build params of build process
   */
  protected def offerBuildParam: BuildParam
}


/**
 * Callable methods exposed by BuildPipeline to the outside.
 *
 * @author Al-assad
 */
trait BuildPipelineExpose {

  /**
   * get current state of the pipeline instance
   */
  def getPipeStatus: PipeStatus

  /**
   * get error of pipeline instance
   */
  def getError: PipeErr

  /**
   * get all of the steps status
   */
  def getStepsStatus: mutable.ListMap[Int, StepStatus]

  /**
   * get current build step index
   */
  def getCurStep: Int

  /**
   * get count of all build steps
   */
  val allSteps: Int

  /**
   * launch the pipeline instance
   */
  def launch(): BuildResult
}


/**
 * Building pipeline trait.
 *
 * @author Al-assad
 */
trait BuildPipeline extends BuildPipelineProcess with BuildPipelineExpose with Logger {

  protected var pipeStatus: PipeStatus = PipeStatus.pending

  protected var error: PipeErr = PipeErr.empty()

  protected var curStep: Int = 0

  protected val stepsStatus: mutable.ListMap[Int, StepStatus] = mutable.ListMap(pipeType.steps.map(
    step => (step._1, StepStatus.waiting)): _*)

  /**
   * use to identify the log record that belongs to which pipeline instance
   */
  protected val logSuffix: String = s"appName=${offerBuildParam.appName}"

  protected var watcher: BuildPipelineWatcher = new SilentPipeWatcherTrait

  def registerWatcher(watcher: BuildPipelineWatcher): BuildPipeline = {
    this.watcher = watcher
    this
  }

  protected def execStep[R](seq: Int)(process: => R): Option[R] = {
    Try {
      curStep = seq
      stepsStatus(seq) = StepStatus.running
      logInfo(s"building pipeline step[$seq/$allSteps] running => ${pipeType.stepsMap(seq)}")
      watcher.onStepStateChange(snapshot)
      process
    } match {
      case Success(result) =>
        stepsStatus(seq) = StepStatus.success
        logInfo(s"building pipeline step[$seq/$allSteps] success")
        watcher.onStepStateChange(snapshot)
        Some(result)
      case Failure(cause) =>
        stepsStatus(seq) = StepStatus.failure
        pipeStatus = PipeStatus.failure
        error = PipeErr(cause.getMessage, cause)
        logInfo(s"building pipeline step[$seq/$allSteps] failure => ${pipeType.stepsMap(seq)}")
        watcher.onStepStateChange(snapshot)
        None
    }
  }

  protected def skipStep(seq: Int): Unit = {
    curStep = seq
    stepsStatus(seq) = StepStatus.skipped
    logInfo(s"building pipeline step[$seq/$allSteps] skipped => ${pipeType.stepsMap(seq)}")
    watcher.onStepStateChange(snapshot)
  }

  /**
   * Launch the building pipeline.
   */
  override def launch(): BuildResult = {
    logInfo(s"building pipeline is launching, params=${offerBuildParam.toString}")
    pipeStatus = PipeStatus.running
    watcher.onStart(snapshot)

    Try(buildProcess()) match {
      case Success(result) =>
        pipeStatus = PipeStatus.success
        logInfo(s"building pipeline has finished successfully.")
        watcher.onFinish(snapshot, result)
        result
      case Failure(cause) =>
        pipeStatus = PipeStatus.failure
        error = PipeErr(cause.getMessage, cause)
        // log and print error trace stack
        logError(s"building pipeline has failed.", cause)
        val result = ErrorResult(error)
        watcher.onFinish(snapshot, result)
        result
    }
  }

  override def getPipeStatus: PipeStatus = PipeStatus.copy(pipeStatus)

  override def getError: PipeErr = error.copy()

  override def getStepsStatus: mutable.ListMap[Int, StepStatus] = stepsStatus.clone()

  override def getCurStep: Int = curStep

  override val allSteps: Int = pipeType.steps.size

  override def logInfo(msg: => String): Unit = super.logInfo(s"[streamx-packer] $msg | $logSuffix")

  override def logError(msg: => String): Unit = super.logError(s"[streamx-packer] $msg | $logSuffix")

  override def logError(msg: => String, throwable: Throwable): Unit = super.logError(s"[streamx-packer] $msg | $logSuffix", throwable)

  /**
   * intercept snapshot
   */
  def snapshot: PipeSnapshot = PipeSnapshot(
    offerBuildParam.appName,
    pipeType,
    getPipeStatus,
    getCurStep,
    allSteps,
    getStepsStatus,
    getError,
    System.currentTimeMillis
  )

}
