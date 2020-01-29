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


import com.streamxhub.common.util.Logger
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.{DataStream, DataStreamSink}
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.http.{Consts, HttpStatus, NameValuePair}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.impl.client.{CloseableHttpClient, DefaultConnectionKeepAliveStrategy, HttpClients}
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object HttpSink {

  def apply(@transient ctx: StreamingContext,
            header: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): HttpSink = new HttpSink(ctx, header, parallelism, name, uid)

}

class HttpSink(@transient ctx: StreamingContext,
               header: Map[String, String],
               parallelism: Int = 0,
               name: String = null,
               uid: String = null)(implicit instance: String = "") extends Sink with Logger {

  def getSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpGet.METHOD_NAME)

  def postSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPost.METHOD_NAME)

  def patchSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPatch.METHOD_NAME)

  def putSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpPut.METHOD_NAME)

  def deleteSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpDelete.METHOD_NAME)

  def optionsSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpOptions.METHOD_NAME)

  def traceSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, HttpTrace.METHOD_NAME)

  private[this] def sink(stream: DataStream[String], method: String): DataStreamSink[String] = {
    val sinkFun = new HttpSinkFunction(header, method)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}


class HttpSinkFunction(header: Map[String, String],
                       method: String,
                       connectTimeout: Int = 5000) extends RichSinkFunction[String] with Logger {

  private var httpClient: CloseableHttpClient = _

  override def open(parameters: Configuration): Unit = {
    httpClient = HttpClients.custom.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE).build
  }

  override def invoke(url: String, context: SinkFunction.Context[_]): Unit = {
    require(url != null)
    val uriAndParams = url.split("\\?")
    val params: ArrayBuffer[NameValuePair] = new ArrayBuffer[NameValuePair]
    uriAndParams.last.split("&").foreach(x => {
      val param = x.split("=")
      params.add(new BasicNameValuePair(param.head, param.last))
    })
    val entity: UrlEncodedFormEntity = new UrlEncodedFormEntity(params.toList, Consts.UTF_8)
    val config: RequestConfig = RequestConfig.custom.setConnectTimeout(connectTimeout).setSocketTimeout(connectTimeout).build

    val uri = uriAndParams.head
    val request: HttpUriRequest = method match {
      case HttpGet.METHOD_NAME =>
        val httpGet = new HttpGet(uri)
        httpGet.setConfig(config)
        httpGet
      case HttpDelete.METHOD_NAME =>
        val httpDelete = new HttpDelete(uri)
        httpDelete.setConfig(config)
        httpDelete
      case HttpOptions.METHOD_NAME =>
        val httpOptions = new HttpOptions(uri)
        httpOptions.setConfig(config)
        httpOptions
      case HttpTrace.METHOD_NAME =>
        val httpTrace = new HttpTrace(uri)
        httpTrace.setConfig(config)
        httpTrace
      case HttpPost.METHOD_NAME =>
        val httpPost: HttpPost = new HttpPost(uri)
        httpPost.setEntity(entity)
        httpPost.setConfig(config)
        httpPost
      case HttpPatch.METHOD_NAME =>
        val httpPatch: HttpPatch = new HttpPatch(uri)
        httpPatch.setEntity(entity)
        httpPatch.setConfig(config)
        httpPatch
      case HttpPut.METHOD_NAME =>
        val httpPut: HttpPut = new HttpPut(uri)
        httpPut.setEntity(entity)
        httpPut.setConfig(config)
        httpPut
    }
    if (header != null && header.nonEmpty) {
      header.foreach { case (k, v) => request.setHeader(k, v) }
    }
    try {
      val response: CloseableHttpResponse = httpClient.execute(request)
      Try(response.getStatusLine.getStatusCode).getOrElse(HttpStatus.SC_EXPECTATION_FAILED) match {
        case HttpStatus.SC_OK =>
        case _ => logError(s"send data error")
      }
    } catch {
      case e: Exception =>
        logError(s"[StreamX] send data error:$e")
    }
  }

  @throws[Exception]
  override def close(): Unit = {
    if (httpClient != null) {
      httpClient.close()
    }
  }
}
