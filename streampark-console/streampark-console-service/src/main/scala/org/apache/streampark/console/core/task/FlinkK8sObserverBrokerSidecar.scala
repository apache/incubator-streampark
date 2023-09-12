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

package org.apache.streampark.console.core.task

import org.apache.streampark.console.core.entity.{Application, FlinkCluster}
import org.apache.streampark.console.core.service.FlinkClusterService
import org.apache.streampark.console.core.service.application.ApplicationInfoService

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper
import zio.{UIO, ZIO}
import zio.ZIO.logError
import zio.ZIOAspect.annotated

import scala.jdk.CollectionConverters._

trait FlinkK8sObserverBrokerSidecar {

  def applicationInfoService: ApplicationInfoService
  def flinkClusterService: FlinkClusterService


  // Get Application record by appId from persistent storage.
  protected def safeGetApplicationRecord(appId: Long): UIO[Option[Application]] = {
    ZIO
      .attemptBlocking(Option(applicationInfoService.getById(appId)))
      .retryN(2)
      .catchAll(err => logError(s"Fail to get Application record: ${err.getMessage}").as(None))
  } @@ annotated("appId" -> appId.toString)

  // Update Application record by appId into persistent storage.
  protected def safeUpdateApplicationRecord(appId: Long)(
      wrapperSetFunc: LambdaUpdateWrapper[Application] => Unit): UIO[Unit] = {
    ZIO
      .attemptBlocking {
        val wrapper = new LambdaUpdateWrapper[Application]()
        wrapperSetFunc(wrapper)
        wrapper.eq((e: Application) => e.getId, appId)
        applicationInfoService.update(null, wrapper)
      }
      .retryN(2)
      .tapError(err => logError(s"Fail to update Application record: ${err.getMessage}"))
      .ignore
  } @@ annotated("appId" -> appId.toString)

  // Get FlinkCluster record by appId from persistent storage.
  protected def safeGetFlinkClusterRecord(id: Long): UIO[Option[FlinkCluster]] = {
    ZIO
      .attemptBlocking(Option(flinkClusterService.getById(id)))
      .retryN(3)
      .catchAll(err => logError(s"Fail to get FlinkCluster record: ${err.getMessage}").as(None))
  } @@ annotated("id" -> id.toString)

  // Update FlinkCluster record by id into persistent storage.
  protected def safeUpdateFlinkClusterRecord(id: Long)(
      wrapperSetFunc: LambdaUpdateWrapper[FlinkCluster] => Unit): UIO[Unit] = {
    ZIO
      .attemptBlocking {
        val wrapper = new LambdaUpdateWrapper[FlinkCluster]()
        wrapperSetFunc(wrapper)
        wrapper.eq((e: FlinkCluster) => e.getId, id)
        flinkClusterService.update(null, wrapper)
      }
      .retryN(3)
      .tapError(err => logError(s"Fail to update FlinkCluster record: ${err.getMessage}"))
      .ignore
  } @@ annotated("id" -> id.toString)


  // Find Application record.
  protected def safeFindApplication(retryN: Int)(
    wrapperFunc: LambdaQueryWrapper[Application] => Unit): UIO[Vector[Application]] = {
    ZIO
      .attemptBlocking {
        val wrapper = new LambdaQueryWrapper[Application]()
        wrapperFunc(wrapper)
        val result = applicationInfoService.list(wrapper)
        if (result == null) Vector.empty[Application] else result.asScala.toVector
      }
      .retryN(retryN)
      .catchAll { err =>
        logError(s"Fail to list Application records: ${err.getMessage}").as(Vector.empty[Application])
      }
  }


}
