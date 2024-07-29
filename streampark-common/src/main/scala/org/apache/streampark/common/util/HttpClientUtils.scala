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

package org.apache.streampark.common.util

import org.apache.streampark.common.util.Implicits._

import org.apache.hc.client5.http.auth.{AuthSchemeFactory, AuthScope, Credentials, StandardAuthScheme}
import org.apache.hc.client5.http.classic.methods.{HttpGet, HttpPost, HttpUriRequestBase}
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.impl.auth.{BasicCredentialsProvider, SPNegoSchemeFactory}
import org.apache.hc.client5.http.impl.classic.{CloseableHttpClient, HttpClientBuilder, HttpClients}
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.URIBuilder

import java.nio.charset.{Charset, StandardCharsets}
import java.security.Principal

object HttpClientUtils {

  private[this] val defaultChart: Charset = StandardCharsets.UTF_8

  private[this] lazy val connectionManager: PoolingHttpClientConnectionManager = {
    val connectionManager = new PoolingHttpClientConnectionManager
    connectionManager.setMaxTotal(50)
    connectionManager.setDefaultMaxPerRoute(5)
    connectionManager
  }

  /** Get HttpClient with connection manager */
  private[this] def getHttpClient =
    HttpClients.custom.setConnectionManager(connectionManager).build

  private[this] def getHttpGet(
      url: String,
      params: JavaMap[String, AnyRef] = null,
      config: RequestConfig = null): HttpGet = {

    val httpGet = params match {
      case null => new HttpGet(url)
      case _ =>
        val uriBuilder = {
          val uriBuilder = new URIBuilder
          uriBuilder.setPath(url)
          uriBuilder.setParameters(paramsToNameValuePairs(params))
          uriBuilder
        }
        new HttpGet(uriBuilder.build)
    }
    if (config != null) {
      httpGet.setConfig(config)
    }
    httpGet
  }

  def httpGetRequest(url: String, config: RequestConfig): String = {
    getHttpResult(getHttpGet(url, null, config))
  }

  def httpGetRequest(
      url: String,
      config: RequestConfig,
      params: JavaMap[String, AnyRef]): String = {
    getHttpResult(getHttpGet(url, params, config))
  }

  def httpGetRequest(
      url: String,
      config: RequestConfig,
      headers: JavaMap[String, AnyRef],
      params: JavaMap[String, AnyRef]): String = {
    val httpGet = getHttpGet(url, params, config)
    headers.entrySet.foreach(p => httpGet.addHeader(p.getKey, String.valueOf(p.getValue)))
    getHttpResult(httpGet)
  }

  def httpPostRequest(url: String): String = {
    val httpPost = new HttpPost(url)
    getHttpResult(httpPost)
  }

  def httpPostRequest(url: String, params: JavaMap[String, AnyRef]): String = {
    val httpPost = new HttpPost(url)
    httpPost.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), defaultChart))
    getHttpResult(httpPost)
  }

  def httpPostRequest(
      url: String,
      params: JavaMap[String, AnyRef],
      headers: JavaMap[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    httpRequest(new HttpPost(url), headers, params)
  }

  private[this] def httpRequest(
      httpUri: HttpUriRequestBase,
      headers: JavaMap[String, AnyRef],
      params: JavaMap[String, AnyRef]) = {
    headers.entrySet.foreach(p => httpUri.addHeader(p.getKey, String.valueOf(p.getValue)))
    httpUri.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), defaultChart))
    getHttpResult(httpUri)
  }

  private[this] def paramsToNameValuePairs(
      params: JavaMap[String, AnyRef]): JavaList[NameValuePair] = {
    params.entrySet
      .map(p => new BasicNameValuePair(p.getKey, p.getValue.toString))
      .toList
  }

  def httpAuthGetRequest(url: String, config: RequestConfig): String = {
    def getHttpAuthClient: CloseableHttpClient = {
      val credentialsProvider = new BasicCredentialsProvider

      val credentials = new Credentials() {
        def getPassword: Array[Char] = null
        def getUserPrincipal: Principal = null
      }

      credentialsProvider.setCredentials(new AuthScope(null, -1), credentials)

      val authSchemeRegistry = RegistryBuilder
        .create[AuthSchemeFactory]
        .register(StandardAuthScheme.SPNEGO, SPNegoSchemeFactory.DEFAULT)
        .build

      HttpClientBuilder
        .create()
        .setDefaultAuthSchemeRegistry(authSchemeRegistry)
        .setDefaultCredentialsProvider(credentialsProvider)
        .setConnectionManager(connectionManager)
        .build()
    }

    getHttpResult(getHttpGet(url, null, config), getHttpAuthClient)
  }

  /** process http request */
  private[this] def getHttpResult(
      request: HttpUriRequestBase,
      httpClient: CloseableHttpClient = getHttpClient): String = {
    val response = httpClient.execute(request)
    val entity = response.getEntity
    if (entity != null) {
      val result = EntityUtils.toString(entity)
      response.close()
      result
    } else null
  }

}
