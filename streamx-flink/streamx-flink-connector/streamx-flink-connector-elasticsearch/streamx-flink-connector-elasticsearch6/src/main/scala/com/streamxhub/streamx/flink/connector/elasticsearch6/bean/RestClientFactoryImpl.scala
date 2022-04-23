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

package com.streamxhub.streamx.flink.connector.elasticsearch6.bean

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.connector.elasticsearch6.conf.ES6Config
import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.RestClientBuilder

class RestClientFactoryImpl(val config: ES6Config) extends RestClientFactory with Logger {
  override def configureRestClientBuilder(restClientBuilder: RestClientBuilder): Unit = {
    //httpClientConfigCallback and requestConfigCallback........
    def configCallback(): RestClientBuilder = {
      val userName = config.userName
      val password = config.password
      //userName,password must be all set,or all not set..
      require(
        (userName != null && password != null) || (userName == null && password == null),
        "[StreamX] elasticsearch auth info error,userName,password must be all set,or all not set."
      )
      val credentialsProvider = (userName, password) match {
        case (null, null) => null
        case _ =>
          val credentialsProvider: CredentialsProvider = new BasicCredentialsProvider()
          credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password))
          credentialsProvider
      }

      val httpClientConfigCallback = new RestClientBuilder.HttpClientConfigCallback {
        override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
          if (credentialsProvider != null) {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            logInfo("elasticsearch auth by userName,password...")
          }
          //other config....
          httpClientBuilder
        }
      }

      val requestConfigCallback = new RestClientBuilder.RequestConfigCallback {
        override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
          if (credentialsProvider != null) {
            requestConfigBuilder.setAuthenticationEnabled(true)
          }
          requestConfigBuilder.setConnectionRequestTimeout(config.connectRequestTimeout)
          requestConfigBuilder.setConnectTimeout(config.connectTimeout)
          requestConfigBuilder.setMaxRedirects(config.maxRedirects)
          requestConfigBuilder.setRedirectsEnabled(config.redirectsEnabled)
          requestConfigBuilder.setConnectTimeout(config.socketTimeout)
          requestConfigBuilder.setRelativeRedirectsAllowed(config.relativeRedirectsAllowed)
          requestConfigBuilder.setContentCompressionEnabled(config.contentCompressionEnabled)
          requestConfigBuilder.setNormalizeUri(config.normalizeUri)
          requestConfigBuilder
        }
      }
      restClientBuilder.setHttpClientConfigCallback(httpClientConfigCallback)
      restClientBuilder.setRequestConfigCallback(requestConfigCallback)
    }

    def setHeader(): RestClientBuilder = {
      val headers = new BasicHeader("Content-Type", config.contentType)
      restClientBuilder.setDefaultHeaders(Array(headers))
      restClientBuilder.setMaxRetryTimeoutMillis(config.maxRetry)
      config.pathPrefix match {
        case null => null
        case path => restClientBuilder.setPathPrefix(path)
      }

    }

    configCallback()

    setHeader()
  }
}
