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

  val pipeType: PipeType

  @throws[Throwable] protected def buildProcess(): BuildResult

  protected def offerBuildParam: BuildParam
}

/**
 * Callable methods exposed by BuildPipeline to the outside.
 *
 * @author Al-assad
 */
trait BuildPipelineExpose {

  def getPipeStatus: PipeStatus

  def getError: PipeErr

  def getStepsStatus: mutable.ListMap[Int, StepStatus]

  def getCurStep: Int

  val allSteps: Int

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
  protected val logSuffix: String = s"appId=${offerBuildParam.appId}, appName=${offerBuildParam.appName}"

  protected def execStep[R](seq: Int)(process: => R): Option[R] = {
    Try {
      curStep = seq
      stepsStatus(seq) = StepStatus.running
      logInfo(s"building pipeline step[$seq/$allSteps] running => ${pipeType.stepsMap(seq)}")
      process
    } match {
      case Success(result) =>
        stepsStatus(seq) = StepStatus.success
        logInfo(s"building pipeline step[$seq/$allSteps] success")
        Some(result)
      case Failure(cause) =>
        stepsStatus(seq) = StepStatus.failure
        pipeStatus = PipeStatus.failure
        error = PipeErr(cause.getMessage, cause)
        logInfo(s"building pipeline step[$seq/$allSteps] failure => ${pipeType.stepsMap(seq)}")
        None
    }
  }

  protected def skipStep(seq: Int): Unit = {
    curStep = seq
    stepsStatus(seq) = StepStatus.skipped
    logInfo(s"building pipeline step[$seq/$allSteps] skipped => ${pipeType.stepsMap(seq)}")
  }

  /**
   * Launch the building pipeline.
   */
  override def launch(): BuildResult = {
    logInfo(s"building pipeline is launching, params=${offerBuildParam.toString}")
    pipeStatus = PipeStatus.running

    Try(buildProcess()) match {
      case Success(result) =>
        pipeStatus = PipeStatus.success
        logInfo(s"building pipeline has finished successfully.")
        result
      case Failure(cause) =>
        pipeStatus = PipeStatus.failure
        error = PipeErr(cause.getMessage, cause)
        // log and print error trace stack
        logError(s"building pipeline has failed.", cause)
        ErrorResult(error)
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

}
