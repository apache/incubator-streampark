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

package com.streamxhub.streamx.flink.packer.pipeline

import com.streamxhub.streamx.common.util.{Logger, ThreadUtils}

import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
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
  def pipeType: PipeType

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
   * StepSeq -> (PipeStepStatus -> status update timestamp)
   */
  def getStepsStatus: Map[Int, (PipeStepStatus, Long)]

  /**
   * get current build step index
   */
  def getCurStep: Int

  /**
   * get count of all build steps
   */
  def allSteps: Int

  /**
   * launch the pipeline instance
   */
  def launch(): BuildResult

  def as[T <: BuildPipeline](clz: Class[T]): T = this.asInstanceOf[T]
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

  protected val stepsStatus: mutable.Map[Int, (PipeStepStatus, Long)] =
    mutable.Map(pipeType.getSteps.asScala.map(e => e._1.toInt -> (PipeStepStatus.waiting -> System.currentTimeMillis)).toSeq: _*)

  /**
   * use to identify the log record that belongs to which pipeline instance
   */
  protected val logSuffix: String = s"appName=${offerBuildParam.appName}"

  protected var watcher: PipeWatcher = new SilentPipeWatcher

  def registerWatcher(watcher: PipeWatcher): BuildPipeline = {
    this.watcher = watcher
    this
  }

  protected def execStep[R](seq: Int)(process: => R): Option[R] = {
    Try {
      curStep = seq
      stepsStatus(seq) = PipeStepStatus.running -> System.currentTimeMillis
      logInfo(s"building pipeline step[$seq/$allSteps] running => ${pipeType.getSteps.get(seq)}")
      watcher.onStepStateChange(snapshot)
      process
    } match {
      case Success(result) =>
        stepsStatus(seq) = PipeStepStatus.success -> System.currentTimeMillis
        logInfo(s"building pipeline step[$seq/$allSteps] success")
        watcher.onStepStateChange(snapshot)
        Some(result)
      case Failure(cause) =>
        stepsStatus(seq) = PipeStepStatus.failure-> System.currentTimeMillis
        pipeStatus = PipeStatus.failure
        error = PipeErr.of(cause.getMessage, cause)
        logInfo(s"building pipeline step[$seq/$allSteps] failure => ${pipeType.getSteps.get(seq)}")
        watcher.onStepStateChange(snapshot)
        None
    }
  }

  protected def skipStep(seq: Int): Unit = {
    curStep = seq
    stepsStatus(seq) = PipeStepStatus.skipped -> System.currentTimeMillis
    logInfo(s"building pipeline step[$seq/$allSteps] skipped => ${pipeType.getSteps.get(seq)}")
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
        error = PipeErr.of(cause.getMessage, cause)
        // log and print error trace stack
        logError(s"building pipeline has failed.", cause)
        val result = ErrorResult()
        watcher.onFinish(snapshot, result)
        result
    }
  }

  override def getPipeStatus: PipeStatus = pipeStatus

  override def getError: PipeErr = error.copy()

  override def getStepsStatus: Map[Int, (PipeStepStatus, Long)] = stepsStatus.toMap

  override def getCurStep: Int = curStep

  override def allSteps: Int = pipeType.getSteps.size

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

object BuildPipeline {

  val execPool = new ThreadPoolExecutor(
    Runtime.getRuntime.availableProcessors * 2,
    300,
    60L,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable](2048),
    ThreadUtils.threadFactory("streamx-pipeline-watcher-executor"),
    new ThreadPoolExecutor.AbortPolicy
  )

  implicit val executor: ExecutionContext = ExecutionContext.fromExecutorService(execPool)

}
