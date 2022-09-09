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

import org.apache.http.NameValuePair
import org.apache.http.auth.{AuthSchemeProvider, AuthScope, Credentials}
import org.apache.http.client.config.{AuthSchemes, RequestConfig}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.client.utils.URIBuilder
import org.apache.http.config.RegistryBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.auth.SPNegoSchemeFactory
import org.apache.http.impl.client.{BasicCredentialsProvider, CloseableHttpClient, HttpClientBuilder, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

import java.nio.charset.{Charset, StandardCharsets}
import java.security.Principal
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
   * Get HttpClient with connection manager
   */
  private[this] def getHttpClient = HttpClients.custom.setConnectionManager(connectionManager).build

  private[this] def getHttpGet(url: String, params: util.Map[String, AnyRef] = null, config: RequestConfig = null): HttpGet = {
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

  def httpGetRequest(url: String, config: RequestConfig, params: util.Map[String, AnyRef]): String = {
    getHttpResult(getHttpGet(url, params, config))
  }

  def httpGetRequest(url: String, config: RequestConfig, headers: util.Map[String, AnyRef], params: util.Map[String, AnyRef]): String = {
    val httpGet = getHttpGet(url, params, config)
    headers.entrySet.foreach(p => httpGet.addHeader(p.getKey, String.valueOf(p.getValue)))
    getHttpResult(httpGet)
  }

  def httpPostRequest(url: String): String = {
    val httpPost = new HttpPost(url)
    getHttpResult(httpPost)
  }

  def httpPatchRequest(url: String): String = {
    val httpPatch = new HttpPatch(url)
    getHttpResult(httpPatch)
  }

  def httpPostRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val httpPost = new HttpPost(url)
    httpPost.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), defaultChart))
    getHttpResult(httpPost)
  }

  def httpPatchRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val httpPatch = new HttpPatch(url)
    httpPatch.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), defaultChart))
    getHttpResult(httpPatch)
  }

  def httpPostRequest(url: String, params: String): String = httpRequest(new HttpPost(url), params)

  def httpPatchRequest(url: String, params: String): String = httpRequest(new HttpPatch(url), params)


  def httpPostRequest(url: String,
                      params: util.Map[String, AnyRef],
                      headers: util.Map[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    httpRequest(new HttpPost(url), headers, params)
  }

  def httpPatchRequest(url: String,
                       params: util.Map[String, AnyRef],
                       headers: util.Map[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    httpRequest(new HttpPatch(url), headers, params)
  }

  private[this] def httpRequest(httpEntity: HttpEntityEnclosingRequestBase, params: String): String = {
    val entity = new StringEntity(params, defaultChart)
    entity.setContentEncoding("UTF-8")
    entity.setContentType("application/json")
    httpEntity.setEntity(entity)
    getHttpResult(httpEntity)
  }

  private[this] def httpRequest(httpPatch: HttpEntityEnclosingRequestBase, headers: util.Map[String, AnyRef], params: util.Map[String, AnyRef]) = {
    headers.entrySet.foreach(p => httpPatch.addHeader(p.getKey, String.valueOf(p.getValue)))
    httpPatch.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), defaultChart))
    getHttpResult(httpPatch)
  }

  private[this] def paramsToNameValuePairs(params: util.Map[String, AnyRef]): util.List[NameValuePair] = {
    val pairs = new util.ArrayList[NameValuePair]
    params.entrySet.foreach(p => pairs.add(new BasicNameValuePair(p.getKey, String.valueOf(p.getValue))))
    pairs
  }

  def httpAuthGetRequest(url: String, config: RequestConfig): String = {
    def getHttpAuthClient: CloseableHttpClient = {
      val credentialsProvider = new BasicCredentialsProvider
      credentialsProvider.setCredentials(
        new AuthScope(null, -1, null),
        new Credentials {
          override def getUserPrincipal: Principal = null

          override def getPassword: String = null
        })

      val authSchemeRegistry = RegistryBuilder.create[AuthSchemeProvider]
        .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true)).build

      HttpClientBuilder.create()
        .setDefaultAuthSchemeRegistry(authSchemeRegistry)
        .setDefaultCredentialsProvider(credentialsProvider)
        .setConnectionManager(connectionManager).build()
    }

    getHttpResult(
      getHttpGet(url, null, config),
      getHttpAuthClient
    )
  }

  /**
   * process http request
   */
  private[this] def getHttpResult(request: HttpRequestBase, httpClient: CloseableHttpClient = getHttpClient): String = {
    try {
      val response = httpClient.execute(request)
      val entity = response.getEntity
      if (entity != null) {
        val result = EntityUtils.toString(entity)
        response.close()
        result
      } else null
    } catch {
      case e: Exception => throw e
    }
  }


}
