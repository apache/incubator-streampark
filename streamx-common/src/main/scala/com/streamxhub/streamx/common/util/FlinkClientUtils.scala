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
package com.streamxhub.streamx.common.util

import org.apache.flink.api.common.Plan
import org.apache.flink.client.program.{PackagedProgram, PackagedProgramUtils, ProgramInvocationException}
import org.apache.flink.configuration.{Configuration, JobManagerOptions}
import org.apache.flink.optimizer.costs.DefaultCostEstimator
import org.apache.flink.optimizer.plan.OptimizedPlan
import org.apache.flink.optimizer.plandump.PlanJSONDumpGenerator
import org.apache.flink.optimizer.{DataStatistics, Optimizer}

import java.net.{InetAddress, InetSocketAddress, ServerSocket}

object FlinkClientUtils {


  /**
   * getExecutionPlan
   *
   * @param packagedProgram
   * @throws
   * @return
   */
  @throws[ProgramInvocationException] def getExecutionPlan(packagedProgram: PackagedProgram): String = {
    require(packagedProgram != null, "[StreamX] FlinkClientUtils.getExecutionPlan: packagedProgram must not be null")
    val address: InetAddress = InetAddress.getLocalHost
    val jmAddress = new InetSocketAddress(address, new ServerSocket(0).getLocalPort)

    val config = new Configuration
    config.setString(JobManagerOptions.ADDRESS, jmAddress.getHostName)
    config.setInteger(JobManagerOptions.PORT, jmAddress.getPort)

    val optimizer = new Optimizer(new DataStatistics, new DefaultCostEstimator, config)
    val plan: Plan = PackagedProgramUtils.getPipelineFromProgram(packagedProgram, config, -1, true).asInstanceOf[Plan]
    val optimizedPlan: OptimizedPlan = optimizer.compile(plan)
    require(optimizedPlan != null, "[StreamX] FlinkClientUtils.getExecutionPlan: optimizedPlan is null")

    val dumper = new PlanJSONDumpGenerator
    dumper.setEncodeForHTML(true)
    dumper.getOptimizerPlanAsJSON(optimizedPlan)
  }

}
