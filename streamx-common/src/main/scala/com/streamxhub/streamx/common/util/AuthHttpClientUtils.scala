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

import com.streamxhub.streamx.common.util.HttpClientUtils.uriBuilder
import org.apache.http.auth.{AuthSchemeProvider, AuthScope, Credentials}
import org.apache.http.client.config.{AuthSchemes, RequestConfig}
import org.apache.http.client.methods.{HttpGet, HttpRequestBase}
import org.apache.http.config.RegistryBuilder
import org.apache.http.impl.auth.SPNegoSchemeFactory
import org.apache.http.impl.client.{BasicCredentialsProvider, CloseableHttpClient, HttpClientBuilder}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.util.EntityUtils

import java.nio.charset.{Charset, StandardCharsets}
import java.security.Principal
import java.util

object AuthHttpClientUtils {

  private[this] val defaultChart: Charset = StandardCharsets.UTF_8

  private[this] lazy val connectionManager: PoolingHttpClientConnectionManager = {
    val connectionManager = new PoolingHttpClientConnectionManager
    connectionManager.setMaxTotal(50)
    connectionManager.setDefaultMaxPerRoute(5)
    connectionManager
  }

  /**
   * 创建 Spengo 认证客户端 for kerberos auth
   *
   * @return SpengoHttpClient
   */
  private[this] def getHttpClient: CloseableHttpClient = {

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

  /**
   * @param url
   * @return
   */
  def httpGetRequest(url: String, config: RequestConfig): String = {
    getResult(getHttpGet(url, null, config))
  }

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
   * 处理Http请求
   *
   * @param request
   * @return
   */
  private[this] def getResult(request: HttpRequestBase): String = {
    try {
      val response = getHttpClient.execute(request)
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




