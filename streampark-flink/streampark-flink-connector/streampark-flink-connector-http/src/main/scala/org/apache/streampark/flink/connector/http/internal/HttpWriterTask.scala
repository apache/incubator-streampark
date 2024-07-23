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

package org.apache.streampark.flink.connector.http.internal

import org.apache.streampark.common.util.{JsonUtils, Logger}
import org.apache.streampark.flink.connector.conf.ThresholdConf
import org.apache.streampark.flink.connector.failover.{FailoverWriter, SinkRequest}

import io.netty.handler.codec.http.HttpHeaders
import org.apache.http.client.methods._
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}

import java.util
import java.util.concurrent.{BlockingQueue, ExecutorService, TimeUnit}

import org.apache.streampark.common.util.Implicits._
import scala.util.Try

case class HttpWriterTask(
    id: Int,
    thresholdConf: ThresholdConf,
    asyncHttpClient: AsyncHttpClient,
    header: Map[String, String],
    queue: BlockingQueue[SinkRequest],
    callbackService: ExecutorService)
  extends Runnable
  with AutoCloseable
  with Logger {

  @volatile var isWorking = false

  val httpMethods: List[String] = List[String](
    HttpGet.METHOD_NAME,
    HttpPost.METHOD_NAME,
    HttpPut.METHOD_NAME,
    HttpPatch.METHOD_NAME,
    HttpDelete.METHOD_NAME,
    HttpOptions.METHOD_NAME,
    HttpTrace.METHOD_NAME
  )

  val failoverWriter: FailoverWriter =
    new FailoverWriter(thresholdConf.storageType, thresholdConf.getFailoverConfig)

  def buildRequest(url: String): Request = {

    val method = httpMethods.filter(x => url.startsWith(x)).head

    val uriAndParams = url.drop(method.length + 3).split("\\?")

    val uri = uriAndParams.head

    val builder = method match {
      case HttpGet.METHOD_NAME => asyncHttpClient.prepareGet(uri)
      case HttpDelete.METHOD_NAME => asyncHttpClient.prepareDelete(uri)
      case HttpOptions.METHOD_NAME => asyncHttpClient.prepareOptions(uri)
      case HttpTrace.METHOD_NAME => asyncHttpClient.prepareTrace(uri)
      case HttpPost.METHOD_NAME => asyncHttpClient.preparePost(uri)
      case HttpPatch.METHOD_NAME => asyncHttpClient.preparePatch(uri)
      case HttpPut.METHOD_NAME => asyncHttpClient.preparePut(uri)
    }
    if (header != null && header.nonEmpty) {
      header.foreach { case (k, v) => builder.setHeader(k, v) }
    }

    Try(uriAndParams(1).trim).getOrElse(null) match {
      case null =>
      case params =>
        val paramMap = new util.HashMap[String, String]()
        params
          .split("&")
          .foreach(
            x => {
              val param = x.split("=")
              paramMap.put(param.head, param.last)
            })
        if (paramMap.nonEmpty) {
          builder.setHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_JSON)
          val json = JsonUtils.write(paramMap)
          builder.setBody(json.getBytes)
        }
    }
    builder.setRequestTimeout(thresholdConf.timeout).build()
  }

  override def run(): Unit =
    try {
      isWorking = true
      logInfo(s"Start writer task, id = $id")
      while (isWorking || queue.nonEmpty) {
        val req = queue.poll(100, TimeUnit.MILLISECONDS)
        if (req != null) {
          val url = req.records.head
          val sinkRequest = SinkRequest(List(url), req.attemptCounter)
          val request = buildRequest(url)
          val whenResponse = asyncHttpClient.executeRequest(request)
          val callback = respCallback(whenResponse, sinkRequest)
          whenResponse.addListener(callback, callbackService)
          if (req.attemptCounter > 0) {
            logInfo(s"get retry url from queue,attemptCounter:${req.attemptCounter}")
          }
        }
      }
    } catch {
      case e: Exception =>
        logError("Error while inserting data", e)
        throw new RuntimeException(e)
    } finally {
      logInfo(s"Task id = $id is finished")
    }

  def respCallback(whenResponse: ListenableFuture[Response], sinkRequest: SinkRequest): Runnable =
    new Runnable {
      override def run(): Unit = {
        Try(whenResponse.get()).getOrElse(null) match {
          case null =>
            logError(
              s"""Error HttpSink executing callback, params = $thresholdConf,can not get Response. """)
            handleFailedResponse(null, sinkRequest)
          case resp if resp.getStatusCode != 200 =>
            logError(
              s"""Error HttpSink executing callback, params = $thresholdConf, StatusCode = ${resp.getStatusCode} """)
            handleFailedResponse(resp, sinkRequest)
          case _ =>
        }
      }
    }

  /**
   * if send data to Http Failed, retry maxRetries, if still failed,flush data to failoverStorage
   *
   * @param response
   * @param sinkRequest
   */
  def handleFailedResponse(response: Response, sinkRequest: SinkRequest): Unit =
    try {
      if (sinkRequest.attemptCounter >= thresholdConf.maxRetries) {
        failoverWriter.write(
          sinkRequest.copy(records = sinkRequest.records.map(_.replaceFirst("^[A-Z]+///", ""))))
        logWarn(
          s"""Failed to send data to Http, Http response = $response. Ready to flush data to ${thresholdConf.storageType}""")
      } else {
        sinkRequest.incrementCounter()
        logWarn(
          s"Next attempt to send data to Http, table = ${sinkRequest.table}, buffer size = ${sinkRequest.size}, current attempt num = ${sinkRequest.attemptCounter}, max attempt num = ${thresholdConf.maxRetries}, response = $response")
        queue.put(sinkRequest)
      }
    } catch {
      case e: Exception => new RuntimeException(s"[StreamPark] handleFailedResponse,error:$e")
    }

  override def close(): Unit = {
    isWorking = false
    failoverWriter.close()
  }
}
