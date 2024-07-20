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

package org.apache.streampark.flink.connector.clickhouse.internal

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.clickhouse.conf.ClickHouseHttpConfig
import org.apache.streampark.flink.connector.failover.{FailoverWriter, SinkRequest}

import io.netty.handler.codec.http.HttpHeaderNames
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}

import java.util.concurrent.{BlockingQueue, ExecutorService, TimeUnit}

import org.apache.streampark.common.util.Implicits._
import scala.util.Try

case class ClickHouseWriterTask(
    id: Int,
    clickHouseConf: ClickHouseHttpConfig,
    asyncHttpClient: AsyncHttpClient,
    queue: BlockingQueue[SinkRequest],
    callbackService: ExecutorService)
  extends Runnable
  with AutoCloseable
  with Logger {

  @volatile var isWorking = false

  val failoverWriter: FailoverWriter =
    new FailoverWriter(clickHouseConf.storageType, clickHouseConf.getFailoverConfig)

  override def run(): Unit =
    try {
      isWorking = true
      logInfo(s"Start writer task, id = $id")
      while (isWorking || queue.nonEmpty) {
        val req = queue.poll(300, TimeUnit.MILLISECONDS)
        if (req != null) {
          send(req)
        }
      }
    } catch {
      case e: Exception =>
        logError("Error while inserting data", e)
        throw new RuntimeException(e)
    } finally {
      logInfo(s"Task id = $id is finished")
    }

  def send(sinkRequest: SinkRequest): Unit = {
    // ClickHouse's http API does not accept EMPTY request body
    if (sinkRequest.sqlStatement == null || sinkRequest.sqlStatement.isEmpty) {
      logWarn(s"Skip empty sql statement")
      return
    }

    val requests = buildRequest(sinkRequest)
    requests.foreach(
      request => {
        logDebug(s"Ready to fire request: $request")
        val whenResponse = asyncHttpClient.executeRequest(request)
        val callback = respCallback(whenResponse, sinkRequest)
        whenResponse.addListener(callback, callbackService)
      })
  }

  private def buildRequest(sinkRequest: SinkRequest): List[Request] = {
    logDebug(s"There is [${sinkRequest.sqlStatement.size}] statement(s) in SinkRequest ")
    // ClickHouse's http API does not accept multiple statements, so requests should be built by splitting statements
    sinkRequest.sqlStatement.filter(_.nonEmpty).map(
      statement => {
        val host = clickHouseConf.getRandomHostUrl
        val builder = asyncHttpClient
          .preparePost(host)
          .setRequestTimeout(clickHouseConf.timeout)
          .setHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
          .setBody(statement)
        if (clickHouseConf.credentials != null) {
          builder.setHeader(HttpHeaderNames.AUTHORIZATION, "Basic " + clickHouseConf.credentials)
        }
        builder.build
      })
  }

  def respCallback(whenResponse: ListenableFuture[Response], sinkRequest: SinkRequest): Runnable =
    new Runnable {
      override def run(): Unit = {
        Try(whenResponse.get()).getOrElse(null) match {
          case null =>
            logError(
              s"""Error ClickHouseSink executing callback, params = $clickHouseConf,can not get Response. """)
            handleFailedResponse(null, sinkRequest)
          case resp if resp.getStatusCode != 200 =>
            logError(
              s"Error ClickHouseSink executing callback, params = $clickHouseConf, StatusCode = ${resp.getStatusCode} ")
            handleFailedResponse(resp, sinkRequest)
          case _ =>
        }
      }
    }

  /**
   * if send data to ClickHouse Failed, retry maxRetries, if still failed,flush data to
   * failoverStorage
   *
   * @param response
   * @param sinkRequest
   */
  def handleFailedResponse(response: Response, sinkRequest: SinkRequest): Unit = {
    if (sinkRequest.attemptCounter > clickHouseConf.maxRetries) {
      logWarn(
        s"""Failed to send data to ClickHouse, cause: limit of attempts is exceeded. ClickHouse response = $response. Ready to flush data to ${clickHouseConf.storageType}""")
      failoverWriter.write(sinkRequest)
      logInfo(
        s"Failover Successful, StorageType = ${clickHouseConf.storageType}, size = ${sinkRequest.size}")
    } else {
      sinkRequest.incrementCounter()
      logWarn(
        s"Next attempt to send data to ClickHouse, table = ${sinkRequest.table}, buffer size = ${sinkRequest.size}, current attempt num = ${sinkRequest.attemptCounter}, max attempt num = ${clickHouseConf.maxRetries}, response = $response")
      queue.put(sinkRequest)
    }
  }

  override def close(): Unit = {
    isWorking = false
    failoverWriter.close()
  }
}
