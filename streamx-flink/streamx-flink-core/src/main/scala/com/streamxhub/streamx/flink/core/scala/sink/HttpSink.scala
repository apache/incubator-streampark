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

package com.streamxhub.streamx.flink.core.scala.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util._
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import com.streamxhub.streamx.flink.core.scala.failover._
import io.netty.handler.codec.http.HttpHeaders
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.http.client.methods._
import org.asynchttpclient._

import java.util.Properties
import java.util.concurrent._
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try


object HttpSink {

  def apply(@(transient@param)
            header: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): HttpSink = new HttpSink(ctx, header, parallelism, name, uid)

}

class HttpSink(@(transient@param) ctx: StreamingContext,
               header: Map[String, String] = Map.empty[String, String],
               parallelism: Int = 0,
               name: String = null,
               uid: String = null) extends Sink {

  def get(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpGet.METHOD_NAME)

  def post(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPost.METHOD_NAME)

  def patch(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPatch.METHOD_NAME)

  def put(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPut.METHOD_NAME)

  def delete(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpDelete.METHOD_NAME)

  def options(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpOptions.METHOD_NAME)

  def trace(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpTrace.METHOD_NAME)

  private[this] def sink(stream: DataStream[String], method: String): DataStreamSink[String] = {
    val params = ctx.parameter.toMap.filter(_._1.startsWith(HTTP_SINK_PREFIX)).map(x => x._1.drop(HTTP_SINK_PREFIX.length + 1) -> x._2)
    val sinkFun = new HttpSinkFunction(params, header, method)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}

class HttpSinkFunction(properties: mutable.Map[String, String],
                       header: Map[String, String],
                       method: String) extends RichSinkFunction[String] with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new ReentrantLock()
  }

  @transient var sinkBuffer: SinkBuffer = _
  @transient var thresholdConf: ThresholdConf = _
  @transient var httpSinkWriter: HttpSinkWriter = _
  @transient var failoverChecker: FailoverChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.lock()
      if (!Lock.initialized) {
        Lock.initialized = true

        val prop: Properties = new Properties()
        properties.foreach { case (k, v) => prop.put(k, v) }
        thresholdConf = ThresholdConf(prop)

        val bufferSize = 1
        val table = properties(KEY_SINK_FAILOVER_TABLE)

        httpSinkWriter = HttpSinkWriter(thresholdConf, header)
        failoverChecker = FailoverChecker(thresholdConf.delayTime)
        sinkBuffer = SinkBuffer(httpSinkWriter, thresholdConf.delayTime, bufferSize, table)
        failoverChecker.addSinkBuffer(sinkBuffer)
        logInfo("HttpSink initialize... ")
      }
      Lock.lock.unlock()
    }
  }

  override def invoke(url: String): Unit = {
    sinkBuffer.put(s"$method///$url")
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


case class HttpSinkWriter(thresholdConf: ThresholdConf, header: Map[String, String]) extends SinkWriter with Logger {
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
  var recordQueue: BlockingQueue[SinkRequest] = new LinkedBlockingQueue[SinkRequest](thresholdConf.queueCapacity)
  var asyncHttpClient: AsyncHttpClient = Dsl.asyncHttpClient

  var service: ExecutorService = Executors.newFixedThreadPool(thresholdConf.numWriters, threadFactory)

  for (i <- 0 until thresholdConf.numWriters) {
    val task = HttpWriterTask(i, thresholdConf, asyncHttpClient, header, recordQueue, callbackService)
    tasks.add(task)
    service.submit(task)
  }

  def write(request: SinkRequest): Unit = try {
    recordQueue.put(request)
  } catch {
    case e: InterruptedException =>
      logError(s"Interrupted error while putting data to queue,error:$e")
      Thread.currentThread.interrupt()
      throw new RuntimeException(e)
  }

  override def close(): Unit = {
    logInfo("Closing HttpSink-writer...")
    tasks.foreach(_.close())
    ThreadUtils.shutdownExecutorService(service)
    ThreadUtils.shutdownExecutorService(callbackService)
    asyncHttpClient.close()
    logInfo(s"${classOf[HttpSinkWriter].getSimpleName} is closed")
  }

}


case class HttpWriterTask(id: Int,
                          thresholdConf: ThresholdConf,
                          asyncHttpClient: AsyncHttpClient,
                          header: Map[String, String],
                          queue: BlockingQueue[SinkRequest],
                          callbackService: ExecutorService) extends Runnable with AutoCloseable with Logger {

  @volatile var isWorking = false

  val httpMethods: List[String] = List[String](
    HttpGet.METHOD_NAME,
    HttpPost.METHOD_NAME,
    HttpPut.METHOD_NAME,
    HttpPatch.METHOD_NAME,
    HttpDelete.METHOD_NAME,
    HttpOptions.METHOD_NAME,
    HttpTrace.METHOD_NAME)

  val failoverWriter: FailoverWriter = new FailoverWriter(thresholdConf.storageType, thresholdConf.getFailoverConfig)

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
        var paramMap = Map[String, String]()
        params.split("&").foreach(x => {
          val param = x.split("=")
          paramMap += param.head -> param.last
        })
        if (paramMap.nonEmpty) {
          builder.setHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_JSON)
          val json = JsonUtils.write(paramMap)
          builder.setBody(json.getBytes)
        }
    }
    builder.setRequestTimeout(thresholdConf.timeout).build()
  }

  override def run(): Unit = try {
    isWorking = true
    logInfo(s"Start writer task, id = ${id}")
    while (isWorking || queue.nonEmpty) {
      val req = queue.poll(100, TimeUnit.MILLISECONDS)
      if (req != null) {
        val url = req.records.head
        val sinkRequest = SinkRequest(List(url), req.table, req.attemptCounter)
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

  def respCallback(whenResponse: ListenableFuture[Response], sinkRequest: SinkRequest): Runnable = new Runnable {
    override def run(): Unit = {
      Try(whenResponse.get()).getOrElse(null) match {
        case null =>
          logError(s"""Error HttpSink executing callback, params = $thresholdConf,can not get Response. """)
          handleFailedResponse(null, sinkRequest)
        case resp if !thresholdConf.successCode.contains(resp.getStatusCode) =>
          logError(s"""Error HttpSink executing callback, params = $thresholdConf, StatusCode = ${resp.getStatusCode} """)
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
  def handleFailedResponse(response: Response, sinkRequest: SinkRequest): Unit = try {
    if (sinkRequest.attemptCounter >= thresholdConf.maxRetries) {
      failoverWriter.write(sinkRequest.copy(records = sinkRequest.records.map(_.replaceFirst("^[A-Z]+///", ""))))
      logWarn(s"""Failed to send data to Http, Http response = $response. Ready to flush data to ${thresholdConf.storageType}""")
    } else {
      sinkRequest.incrementCounter()
      logWarn(s"Next attempt to send data to Http, table = ${sinkRequest.table}, buffer size = ${sinkRequest.size}, current attempt num = ${sinkRequest.attemptCounter}, max attempt num = ${thresholdConf.maxRetries}, response = $response")
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

