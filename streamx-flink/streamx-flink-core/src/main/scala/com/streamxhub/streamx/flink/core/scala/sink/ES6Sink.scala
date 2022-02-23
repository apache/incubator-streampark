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
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkBase._
import org.apache.flink.streaming.connectors.elasticsearch.{ActionRequestFailureHandler, ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch6.{ElasticsearchSink, RestClientFactory}
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.message.BasicHeader
import org.elasticsearch.action.ActionRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RestClientBuilder

import java.util.Properties
import java.util.function.BiConsumer
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.Map
import scala.util.{Success, Try}


object ES6Sink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): ES6Sink = new ES6Sink(ctx, property, parallelism, name, uid)

}


class ES6Sink(@(transient@param) ctx: StreamingContext,
              property: Properties = new Properties(),
              parallelism: Int = 0,
              name: String = null,
              uid: String = null) extends Sink with Logger {


   def sink[T](stream: DataStream[T],
                          suffix: String,
                          restClientFactory: RestClientFactory,
                          failureHandler: ActionRequestFailureHandler,
                          f: T => ActionRequest): DataStreamSink[T] = {

    //所有es的配置文件...
    val fullConfig = ctx.parameter.toMap
      .filter(_._1.startsWith(ES_PREFIX))
      .filter(_._2.nonEmpty)

    //当前实例(默认,或者指定后缀实例)的配置文件...
    val shortConfig = fullConfig
      .filter(_._1.endsWith(suffix))
      .map(x => x._1.drop(ES_PREFIX.length + suffix.length) -> x._2.trim)

    // parameter of sink.es.host
    val httpHosts = shortConfig.getOrElse(KEY_HOST, SIGN_EMPTY).split(SIGN_COMMA).map(x => {
      x.split(SIGN_COLON) match {
        case Array(host, port) => new HttpHost(host, port.toInt)
      }
    })

    require(httpHosts.nonEmpty, "elasticsearch config error,please check, e.g: sink.es.host=$host1:$port1,$host2:$port2")

    val sinkFunc: ElasticsearchSinkFunction[T] = new ElasticsearchSinkFunction[T] {
      def createIndexRequest(element: T): ActionRequest = f(element)

      override def process(element: T, runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = {
        val request: ActionRequest = createIndexRequest(element)
        request match {
          case indexRequest if indexRequest.isInstanceOf[IndexRequest] => requestIndexer.add(indexRequest.asInstanceOf[IndexRequest])
          case deleteRequest if deleteRequest.isInstanceOf[DeleteRequest] => requestIndexer.add(deleteRequest.asInstanceOf[DeleteRequest])
          case updateRequest if updateRequest.isInstanceOf[UpdateRequest] => requestIndexer.add(updateRequest.asInstanceOf[UpdateRequest])
          case _ => {
            logError("ElasticsearchSinkFunction add ActionRequest is Deprecated plasase use IndexRequest|DeleteRequest|UpdateRequest ")
            requestIndexer.add(request)
          }
        }
      }
    }

    val sinkBuilder = new ElasticsearchSink.Builder[T](httpHosts.toList, sinkFunc)
    // failureHandler
    sinkBuilder.setFailureHandler(failureHandler)
    //restClientFactory
    if (restClientFactory == null) {
      val restClientFactory = new RestClientFactoryImpl(fullConfig)
      sinkBuilder.setRestClientFactory(restClientFactory)
    } else {
      sinkBuilder.setRestClientFactory(restClientFactory)
    }

    def doConfig(param: (String, String)): Unit = param match {
      // parameter of sink.es.bulk.flush.max.actions
      case (CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS, v) => sinkBuilder.setBulkFlushMaxActions(v.toInt)
      // parameter of sink.es.bulk.flush.max.size.mb
      case (CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB, v) => sinkBuilder.setBulkFlushMaxSizeMb(v.toInt)
      // parameter of sink.es.bulk.flush.interval.ms
      case (CONFIG_KEY_BULK_FLUSH_INTERVAL_MS, v) => sinkBuilder.setBulkFlushInterval(v.toInt)
      // parameter of sink.es.bulk.flush.backoff.enable
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE, v) => sinkBuilder.setBulkFlushBackoff(v.toBoolean)
      // parameter of sink.es.bulk.flush.backoff.type value of [ CONSTANT or EXPONENTIAL ]
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE, v) => sinkBuilder.setBulkFlushBackoffType(FlushBackoffType.valueOf(v))
      // parameter of sink.es.bulk.flush.backoff.retries
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES, v) => sinkBuilder.setBulkFlushBackoffRetries(v.toInt)
      // parameter of sink.es.bulk.flush.backoff.delay
      case (CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY, v) => sinkBuilder.setBulkFlushBackoffDelay(v.toLong)
      // other...
      case _ =>
    }
    //set value from properties
    shortConfig.filter(_._1.startsWith(KEY_ES_BULK_PREFIX)).foreach(doConfig)
    //set value from method parameter...
    property.forEach(new BiConsumer[Object, Object] {
      override def accept(k: Object, v: Object): Unit = doConfig(k.toString, v.toString)
    })

    val esSink: ElasticsearchSink[T] = sinkBuilder.build()
    if (shortConfig.getOrElse(KEY_ES_DISABLE_FLUSH_ONCHECKPOINT, "false").toBoolean) {
      esSink.disableFlushOnCheckpoint()
    }
    val sink = stream.addSink(esSink)
    afterSink(sink, parallelism, name, uid)
  }
}

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
