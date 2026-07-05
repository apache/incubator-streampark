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

package org.apache.streampark.connector.doris.conf;

import org.apache.streampark.common.constants.Constants;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

/** Doris sink configuration. */
public class DorisConfig implements Serializable {

    public static final String CSV = "csv";
    public static final String JSON = "json";

    public final DorisSinkConfigOption sinkOption;
    public final String user;
    public final String password;
    public final List<String> loadUrl;
    public final String loadFormat;
    public final String rowDelimiter;
    public final int timeout;
    public final int sinkMaxRow;
    public final int sinkMaxBytes;
    public final int sinkMaxRetries;
    public final long flushInterval;
    public final long sinkOfferTimeout;
    public final String labelPrefix;
    public final String semantic;
    public final String database;
    public final String table;

    private long currentHostId = 0;

    public DorisConfig(Properties parameters) {
        Properties prop = parameters != null ? parameters : new Properties();
        this.sinkOption = DorisSinkConfigOption.of(prop);
        this.user = sinkOption.user.get();
        this.password = sinkOption.password.get();
        this.loadUrl = sinkOption.loadUrl.get();
        this.loadFormat = sinkOption.loadFormat.get();
        this.rowDelimiter = sinkOption.rowDelimiter.get();
        this.timeout = sinkOption.connectTimeout.get();
        this.sinkMaxRow = sinkOption.maxRow.get();
        this.sinkMaxBytes = sinkOption.maxBytes.get();
        this.sinkMaxRetries = sinkOption.maxRetries.get();
        this.flushInterval = sinkOption.flushInterval.get();
        this.sinkOfferTimeout = sinkOption.sinkOfferTimeout.get();
        this.labelPrefix = sinkOption.labelPrefix.get();
        this.semantic = sinkOption.semantic.get();
        this.database = sinkOption.database.get();
        this.table = sinkOption.table.get();
    }

    public static DorisConfig of(Properties properties) {
        return new DorisConfig(properties);
    }

    public Properties loadProperties() {
        return sinkOption.getInternalProperties();
    }

    public int getLoadUrlSize() {
        return loadUrl.size();
    }

    public String getHostUrl() {
        currentHostId += 1;
        return loadUrl.get((int) (currentHostId % loadUrl.size()));
    }

    @Override
    public String toString() {
        return String.format(
                "{ doris user: %s, password: %s, hosts: %s }",
                user, Constants.DEFAULT_DATAMASK_STRING, String.join(",", loadUrl));
    }

    public static String CSV() {
        return CSV;
    }

    public static String JSON() {
        return JSON;
    }

    public DorisSinkConfigOption sinkOption() {
        return sinkOption;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    public String loadFormat() {
        return loadFormat;
    }

    public String rowDelimiter() {
        return rowDelimiter;
    }

    public int timeout() {
        return timeout;
    }

    public int sinkMaxRow() {
        return sinkMaxRow;
    }

    public int sinkMaxBytes() {
        return sinkMaxBytes;
    }

    public int sinkMaxRetries() {
        return sinkMaxRetries;
    }

    public long flushInterval() {
        return flushInterval;
    }

    public long sinkOfferTimeout() {
        return sinkOfferTimeout;
    }

    public String labelPrefix() {
        return labelPrefix;
    }

    public String semantic() {
        return semantic;
    }

    public String database() {
        return database;
    }

    public String table() {
        return table;
    }
}
