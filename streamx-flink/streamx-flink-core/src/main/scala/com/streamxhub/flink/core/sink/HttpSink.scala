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

  def getSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "get")

  def postSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "post")

  def patchSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "patch")

  def putSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "put")

  def deleteSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "delete")

  def optionsSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "options")

  def traceSink(stream: DataStream[String]): DataStreamSink[String] = sink(stream, "trace")

  private[this] def sink(stream: DataStream[String], method: String = "post"): DataStreamSink[String] = {
    val sinkFun = new HttpSinkFunction[String](header, method)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }
}


class HttpSinkFunction[T](header: Map[String, String],
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
    val request: HttpUriRequest = method.toLowerCase match {
      case "get" =>
        val httpGet = new HttpGet(uri)
        httpGet.setConfig(config)
        httpGet
      case "delete" =>
        val httpDelete = new HttpDelete(uri)
        httpDelete.setConfig(config)
        httpDelete
      case "options" =>
        val httpOptions = new HttpOptions(uri)
        httpOptions.setConfig(config)
        httpOptions
      case "trace" =>
        val httpTrace = new HttpTrace(uri)
        httpTrace.setConfig(config)
        httpTrace
      case "post" =>
        val httpPost: HttpPost = new HttpPost(uri)
        httpPost.setEntity(entity)
        httpPost.setConfig(config)
        httpPost
      case "patch" =>
        val httpPatch: HttpPatch = new HttpPatch(uri)
        httpPatch.setEntity(entity)
        httpPatch.setConfig(config)
        httpPatch
      case "put" =>
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
