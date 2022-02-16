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

import org.apache.http.NameValuePair
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.nio.charset.{Charset, StandardCharsets}
import java.util
import scala.collection.JavaConversions._

object HttpClientUtils {

  private[this] val defaultChart: Charset = StandardCharsets.UTF_8

  private[this] lazy val connectionManager: PoolingHttpClientConnectionManager = {
    val connectionManager = new PoolingHttpClientConnectionManager
    connectionManager.setMaxTotal(50)
    connectionManager.setDefaultMaxPerRoute(5)
    connectionManager
  }

  /**
   * 通过连接池获取HttpClient
   *
   * @return
   */
  private[this] def getHttpClient = HttpClients.custom.setConnectionManager(connectionManager).build

  private[this] def getHttpGet(url: String, params: util.Map[String, AnyRef] = null, config: RequestConfig = null): HttpGet = {
    val httpGet = params match {
      case null => new HttpGet(url)
      case _ =>
        val ub = uriBuilder(url, params)
        new HttpGet(ub.build)
    }
    if (config != null) {
      httpGet.setConfig(config)
    }
    httpGet
  }

  /**
   * @param url
   * @return
   */
  def httpGetRequest(url: String, config: RequestConfig): String = {
    getResult(getHttpGet(url, null, config))
  }

  @throws[URISyntaxException] def httpGetRequest(url: String, config: RequestConfig, params: util.Map[String, AnyRef]): String = {
    getResult(getHttpGet(url, params, config))
  }

  @throws[URISyntaxException] def httpGetRequest(url: String, config: RequestConfig, headers: util.Map[String, AnyRef], params: util.Map[String, AnyRef]): String = {
    val httpGet = getHttpGet(url, params, config)
    for (param <- headers.entrySet) {
      httpGet.addHeader(param.getKey, String.valueOf(param.getValue))
    }
    getResult(httpGet)
  }

  private[this] def uriBuilder(url: String, params: util.Map[String, AnyRef]): URIBuilder = {
    val uriBuilder = new URIBuilder
    uriBuilder.setPath(url)
    uriBuilder.setParameters(params2NVPS(params))
    uriBuilder
  }

  def httpPostRequest(url: String): String = {
    val httpPost = new HttpPost(url)
    getResult(httpPost)
  }

  def httpPatchRequest(url: String): String = {
    val httpPatch = new HttpPatch(url)
    getResult(httpPatch)
  }

  @throws[UnsupportedEncodingException] def httpPostRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val httpPost = new HttpPost(url)
    httpPost.setEntity(new UrlEncodedFormEntity(params2NVPS(params), defaultChart))
    getResult(httpPost)
  }

  @throws[UnsupportedEncodingException] def httpPatchRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val httpPatch = new HttpPatch(url)
    httpPatch.setEntity(new UrlEncodedFormEntity(params2NVPS(params), defaultChart))
    getResult(httpPatch)
  }

  @throws[UnsupportedEncodingException] def httpPostRequest(url: String, params: String): String = httpRequest(new HttpPost(url), params)

  @throws[UnsupportedEncodingException] def httpPatchRequest(url: String, params: String): String = httpRequest(new HttpPatch(url), params)


  @throws[UnsupportedEncodingException] def httpPostRequest(url: String,
                                                            params: util.Map[String, AnyRef],
                                                            headers: util.Map[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    httpRequest(new HttpPost(url), headers, params)
  }

  @throws[UnsupportedEncodingException] def httpPatchRequest(url: String,
                                                             params: util.Map[String, AnyRef],
                                                             headers: util.Map[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    httpRequest(new HttpPatch(url), headers, params)
  }

  private[this] def httpRequest(httpEntity: HttpEntityEnclosingRequestBase, params: String): String = {
    val entity = new StringEntity(params, defaultChart) //解决中文乱码问题
    entity.setContentEncoding("UTF-8")
    entity.setContentType("application/json")
    httpEntity.setEntity(entity)
    getResult(httpEntity)
  }

  private[this] def httpRequest(httpPatch: HttpEntityEnclosingRequestBase, headers: util.Map[String, AnyRef], params: util.Map[String, AnyRef]) = {
    for (param <- headers.entrySet) {
      httpPatch.addHeader(param.getKey, String.valueOf(param.getValue))
    }
    httpPatch.setEntity(new UrlEncodedFormEntity(params2NVPS(params), defaultChart))
    getResult(httpPatch)
  }

  private[this] def params2NVPS(params: util.Map[String, AnyRef]): util.List[NameValuePair] = {
    val pairs = new util.ArrayList[NameValuePair]
    for (param <- params.entrySet) {
      pairs.add(new BasicNameValuePair(param.getKey, String.valueOf(param.getValue)))
    }
    pairs
  }

  /**
   * 处理Http请求
   *
   * @param request
   * @return
   */
  private[this] def getResult(request: HttpRequestBase): String = {
    val httpClient = getHttpClient
    try {
      val response = httpClient.execute(request)
      val entity = response.getEntity
      if (entity != null) { // long len = entity.getContentLength();// -1 表示长度未知
        val result = EntityUtils.toString(entity)
        response.close()
        result
      } else null
    } catch {
      case e: Exception => throw e
    }
  }

}
