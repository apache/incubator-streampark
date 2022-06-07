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

package com.streamxhub.streamx.flink.connector.elasticsearch7.conf

import org.apache.http.HttpHost

import java.util.Properties

class ES7Config(parameters: Properties) extends Serializable {

  val sinkOption: ES7SinkConfigOption = ES7SinkConfigOption(properties = parameters)

  val disableFlushOnCheckpoint: Boolean = sinkOption.disableFlushOnCheckpoint.get()

  val host: List[HttpHost] = sinkOption.host.get().toList

  val userName: String = sinkOption.userName.get()

  val password: String = sinkOption.password.get()

  val connectRequestTimeout: Int = sinkOption.connectRequestTimeout.get()

  val connectTimeout: Int = sinkOption.connectTimeout.get()

  val contentType: String = sinkOption.contentType.get()

  val pathPrefix: String = sinkOption.pathPrefix.get()

  val staleConnectionCheckEnabled: Boolean = sinkOption.staleConnectionCheckEnabled.get()

  val redirectsEnabled: Boolean = sinkOption.redirectsEnabled.get()

  val maxRedirects: Int = sinkOption.maxRedirects.get()

  val relativeRedirectsAllowed: Boolean = sinkOption.relativeRedirectsAllowed.get()

  val authenticationEnabled: Boolean = sinkOption.authenticationEnabled.get()

  val socketTimeout: Int = sinkOption.socketTimeout.get()

  val contentCompressionEnabled: Boolean = sinkOption.contentCompressionEnabled.get()

  val normalizeUri: Boolean = sinkOption.normalizeUri.get()


}
