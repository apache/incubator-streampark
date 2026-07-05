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

package org.apache.streampark.flink.connector.elasticsearch6.conf;

import org.apache.http.HttpHost;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ES6Config implements Serializable {
    public final ES6SinkConfigOption sinkOption;
    public final boolean disableFlushOnCheckpoint;
    public final List<HttpHost> host;
    public final String userName;
    public final String password;
    public final int connectRequestTimeout;
    public final int connectTimeout;
    public final int maxRetry;
    public final String contentType;
    public final String pathPrefix;
    public final boolean staleConnectionCheckEnabled;
    public final boolean redirectsEnabled;
    public final int maxRedirects;
    public final boolean relativeRedirectsAllowed;
    public final boolean authenticationEnabled;
    public final int socketTimeout;
    public final boolean contentCompressionEnabled;
    public final boolean normalizeUri;

    public ES6Config(Properties parameters) {
        this.sinkOption = ES6SinkConfigOption.of(parameters);
        this.disableFlushOnCheckpoint = sinkOption.disableFlushOnCheckpoint.get();
        this.host = Arrays.asList(sinkOption.host.get());
        this.userName = sinkOption.userName.get();
        this.password = sinkOption.password.get();
        this.connectRequestTimeout = sinkOption.connectRequestTimeout.get();
        this.connectTimeout = sinkOption.connectTimeout.get();
        this.maxRetry = sinkOption.maxRetry.get();
        this.contentType = sinkOption.contentType.get();
        this.pathPrefix = sinkOption.pathPrefix.get();
        this.staleConnectionCheckEnabled = sinkOption.staleConnectionCheckEnabled.get();
        this.redirectsEnabled = sinkOption.redirectsEnabled.get();
        this.maxRedirects = sinkOption.maxRedirects.get();
        this.relativeRedirectsAllowed = sinkOption.relativeRedirectsAllowed.get();
        this.authenticationEnabled = sinkOption.authenticationEnabled.get();
        this.socketTimeout = sinkOption.socketTimeout.get();
        this.contentCompressionEnabled = sinkOption.contentCompressionEnabled.get();
        this.normalizeUri = sinkOption.normalizeUri.get();
    }
}
