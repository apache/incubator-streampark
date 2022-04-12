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

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.Logger
import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.RestClientBuilder

import scala.collection.Map
import scala.util.{Success, Try}

class RestClientFactoryImpl(val config: Map[String, String]) extends RestClientFactory with Logger {
  override def configureRestClientBuilder(restClientBuilder: RestClientBuilder): Unit = {
    //httpClientConfigCallback and requestConfigCallback........
    def configCallback(): RestClientBuilder = {
      val userName = config.getOrElse(KEY_ES_AUTH_USER, null)
      val password = config.getOrElse(KEY_ES_AUTH_PASSWORD, null)
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
          config.foreach {
            case (KEY_ES_CONN_REQ_TIME_OUT, v) => requestConfigBuilder.setConnectionRequestTimeout(v.toInt)
            case (KEY_ES_CONN_TIME_OUT, v) => requestConfigBuilder.setConnectTimeout(v.toInt)
            case _ =>
          }
          //other config....
          requestConfigBuilder
        }
      }
      restClientBuilder.setHttpClientConfigCallback(httpClientConfigCallback)
      restClientBuilder.setRequestConfigCallback(requestConfigCallback)
    }

    def setHeader(): RestClientBuilder = {
      val contentType = config.getOrElse(KEY_ES_REST_CONTENT_TYPE, "application/json")
      val maxRetry = Try(config.get(KEY_ES_REST_MAX_RETRY).toString.toInt) match {
        case Success(value) => value
        case _ =>
          logWarn(s" config error: $KEY_ES_REST_MAX_RETRY is not set or invalid,use 10000 ")
          10000
      }

      val headers = new BasicHeader("Content-Type", contentType)
      restClientBuilder.setDefaultHeaders(Array(headers))
      restClientBuilder.setMaxRetryTimeoutMillis(maxRetry)
      config.getOrElse(KEY_ES_REST_PATH_PREFIX, null) match {
        case null => null
        case path => restClientBuilder.setPathPrefix(path)
      }

    }

    configCallback()

    setHeader()
  }
}
