/**
 * Copyright (c) 2019 The StreamX Project
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
package com.streamxhub.flink.core.sink


import java.util.Properties
import java.util.concurrent._

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util._
import com.streamxhub.flink.core.failover._
import org.apache.http.client.methods._

import scala.collection.JavaConversions._
import com.streamxhub.flink.core.StreamingContext
import io.netty.handler.codec.http.HttpHeaders
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.scala.DataStream
import org.asynchttpclient.{AsyncHttpClient, Dsl, ListenableFuture, Request, Response}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try


object HttpSink {

  def apply(@transient ctx: StreamingContext,
            header: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): HttpSink = new HttpSink(ctx, header, parallelism, name, uid)

}

class HttpSink(@transient ctx: StreamingContext,
               header: Map[String, String] = Map.empty[String, String],
               parallelism: Int = 0,
               name: String = null,
               uid: String = null)(implicit instance: String = "") extends Sink {

  def getSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpGet.METHOD_NAME)

  def postSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPost.METHOD_NAME)

  def patchSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPatch.METHOD_NAME)

  def putSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPut.METHOD_NAME)

  def deleteSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpDelete.METHOD_NAME)

  def optionsSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpOptions.METHOD_NAME)

  def traceSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpTrace.METHOD_NAME)

  private[this] def sink(stream: DataStream[String], method: String): DataStreamSink[String] = {
    val params = ctx.paramMap.filter(_._1.startsWith(HTTP_SINK_PREFIX)).map(x => {
      x._1.drop(HTTP_SINK_PREFIX.length + 1) -> x._2
    })
    val sinkFun = new HttpSinkFunction(params, header, method)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}

class HttpSinkFunction(properties: mutable.Map[String, String],
                       header: Map[String, String],
                       method: String,
                       connectTimeout: Int = 5000) extends RichSinkFunction[String] with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new Object()
  }

  @transient var sinkBuffer: SinkBuffer = _
  @transient var failoverConf: FailoverConf = _
  @transient var httpSinkWriter: HttpSinkWriter = _
  @transient var failoverChecker: FailoverChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.synchronized {
        if (!Lock.initialized) {
          Lock.initialized = true

          val bufferSize = 1
          val checkTime = properties(KEY_SINK_THRESHOLD_CHECK_TIME).toInt
          val requestTimeout = properties.getOrElse(KEY_SINK_THRESHOLD_REQ_TIMEOUT, DEFAULT_SINK_REQUEST_TIMEOUT).toString.toInt
          val table = properties(KEY_SINK_FAILOVER_TABLE)

          val prop: Properties = new Properties()
          properties.foreach { case (k, v) => prop.put(k, v) }
          failoverConf = FailoverConf(prop)

          httpSinkWriter = HttpSinkWriter(failoverConf, header, requestTimeout)
          failoverChecker = FailoverChecker(checkTime)
          sinkBuffer = SinkBuffer(httpSinkWriter, checkTime, bufferSize, table)
          failoverChecker.addSinkBuffer(sinkBuffer)
          logInfo("[StreamX] HttpSink initialize... ")
        }
      }
    }
  }

  override def invoke(url: String): Unit = {
    sinkBuffer.put(s"$method//$url")
  }

  override def close(): Unit = {
    if (!isClosed) {
      Lock.lock.synchronized {
        if (!isClosed) {
          if (sinkBuffer != null) sinkBuffer.close()
          if (httpSinkWriter != null) httpSinkWriter.close()
          if (failoverChecker != null) failoverChecker.close()
          isClosed = true
          super.close()
        }
      }
    }
  }
}


case class HttpSinkWriter(failoverConf: FailoverConf, header: Map[String, String], requestTimeout: Int = 2000) extends SinkWriter with Logger {
  private val callbackServiceFactory = ThreadUtils.threadFactory("HttpSink-writer-callback-executor")
  private val threadFactory: ThreadFactory = ThreadUtils.threadFactory("HttpSink-writer")

  var callbackService: ExecutorService = new ThreadPoolExecutor(
    math.max(Runtime.getRuntime.availableProcessors / 4, 2),
    Integer.MAX_VALUE,
    60L,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue[Runnable],
    callbackServiceFactory
  )

  var tasks: ListBuffer[HttpWriterTask] = ListBuffer[HttpWriterTask]()
  var commonQueue: BlockingQueue[SinkRequest] = new LinkedBlockingQueue[SinkRequest](failoverConf.queueMaxCapacity)
  var asyncHttpClient: AsyncHttpClient = Dsl.asyncHttpClient

  var service: ExecutorService = Executors.newFixedThreadPool(failoverConf.numWriters, threadFactory)

  for (i <- 0 until failoverConf.numWriters) {
    val task = HttpWriterTask(i, asyncHttpClient, header, requestTimeout, commonQueue, failoverConf, callbackService)
    tasks.add(task)
    service.submit(task)
  }

  def write(params: SinkRequest): Unit = try {
    commonQueue.put(params)
  } catch {
    case e: InterruptedException =>
      logError(s"[StreamX] Interrupted error while putting data to queue,error:$e")
      Thread.currentThread.interrupt()
      throw new RuntimeException(e)
  }

  override def close(): Unit = {
    logInfo("[StreamX] Closing HttpSink-writer...")
    tasks.foreach(_.close())
    ThreadUtils.shutdownExecutorService(service)
    ThreadUtils.shutdownExecutorService(callbackService)
    commonQueue.clear()
    tasks.clear()
    logInfo(s"[StreamX] ${classOf[HttpSinkWriter].getSimpleName} is closed")
  }

}


case class HttpWriterTask(id: Int,
                          asyncHttpClient: AsyncHttpClient,
                          header: Map[String, String],
                          connectTimeout: Int,
                          queue: BlockingQueue[SinkRequest],
                          failoverConf: FailoverConf,
                          callbackService: ExecutorService) extends Runnable with AutoCloseable with Logger {
  val HTTP_OK = 200
  @volatile var isWorking = false

  val failoverWriter: FailoverWriter = new FailoverWriter(failoverConf.failoverStorage, failoverConf.getFailoverConfig)

  def buildRequest(url: String): Request = {

    val methods = Array[String](
      HttpGet.METHOD_NAME,
      HttpPost.METHOD_NAME,
      HttpPut.METHOD_NAME,
      HttpPatch.METHOD_NAME,
      HttpDelete.METHOD_NAME,
      HttpOptions.METHOD_NAME,
      HttpTrace.METHOD_NAME)

    val method = methods.filter(x => url.startsWith(x)).head

    val uriAndParams = url.drop(method.length + 2).split("\\?")

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
        var paramMap = Map[String, String]()
        params.split("&").foreach(x => {
          val param = x.split("=")
          paramMap += param.head -> param.last
        })
        if (paramMap.nonEmpty) {
          builder.setHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_JSON)
          builder.setBody(JsonUtils.write(paramMap).getBytes)
        }
    }
    builder.build()
  }

  override def run(): Unit = try {
    isWorking = true
    logInfo(s"[StreamX] Start writer task, id = ${id}")
    while (isWorking || queue.nonEmpty) {
      val req = queue.poll(300, TimeUnit.MILLISECONDS)
      if (req != null) {
        val url = req.records.head
        val request = buildRequest(url)
        val whenResponse = asyncHttpClient.executeRequest(request)
        val sinkRequest = SinkRequest(List(url), req.table)
        val callback = respCallback(whenResponse, sinkRequest)
        whenResponse.addListener(callback, callbackService)
      }
    }
  } catch {
    case e: Exception =>
      logError("[StreamX] Error while inserting data", e)
      throw new RuntimeException(e)
  } finally {
    logInfo(s"[StreamX] Task id = $id is finished")
  }

  def respCallback(whenResponse: ListenableFuture[Response], chRequest: SinkRequest): Runnable = new Runnable {
    override def run(): Unit = {
      Try(whenResponse.get()).getOrElse(null) match {
        case null =>
          logError(s"""[StreamX] Error HttpSink executing callback, params = $failoverConf,can not get Response. """)
          handleFailedResponse(null, chRequest)
        case resp if resp.getStatusCode != HTTP_OK =>
          logError(s"""[StreamX] Error HttpSink executing callback, params = $failoverConf, StatusCode = ${resp.getStatusCode} """)
          handleFailedResponse(resp, chRequest)
        case _ =>
      }
    }
  }

  /**
   * if send data to Http Failed, retry $maxRetries, if still failed,flush data to $failoverStorage
   *
   * @param response
   * @param sinkRequest
   */
  def handleFailedResponse(response: Response, sinkRequest: SinkRequest): Unit = try {
    if (sinkRequest.attemptCounter > failoverConf.maxRetries) {
      logWarning(s"""[StreamX] Failed to send data to Http, cause: limit of attempts is exceeded. Http response = $response. Ready to flush data to ${failoverConf.failoverStorage}""")
      failoverWriter.write(sinkRequest)
      logInfo(s"[StreamX] failover Successful, StorageType = ${failoverConf.failoverStorage}, size = ${sinkRequest.size}")
    } else {
      sinkRequest.incrementCounter()
      logWarning(s"[StreamX] Next attempt to send data to Http, table = ${sinkRequest.table}, buffer size = ${sinkRequest.size}, current attempt num = ${sinkRequest.attemptCounter}, max attempt num = ${failoverConf.maxRetries}, response = $response")
      queue.put(sinkRequest)
    }
  } catch {
    case e: Exception => new RuntimeException(s"[StreamX] handleFailedResponse,error:$e")
  }


  override def close(): Unit = {
    isWorking = false
    failoverWriter.close()
  }
}

