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

package org.apache.streampark.flink.connector.redis.conf;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Redis connector configuration. */
public class RedisConfig implements Serializable {

    public final RedisSinkConfigOption sinkOption;
    public final String connectType;
    public final String host;
    public final int port;
    public final Set<String> sentinels;

    public RedisConfig(Properties parameters) {
        this.sinkOption = RedisSinkConfigOption.of(parameters);
        this.connectType = sinkOption.connectType.get();
        this.host = sinkOption.host.get();
        this.port = sinkOption.port.get();
        if (RedisSinkConfigOption.DEFAULT_CONNECT_TYPE.equals(connectType)) {
            this.sentinels = new HashSet<>();
        } else {
            this.sentinels =
                    Arrays.stream(host.split(sinkOption.getSignComma()))
                            .map(
                                    x -> {
                                        if (x.contains(":")) {
                                            return x;
                                        }
                                        throw new IllegalArgumentException(
                                                "Redis sentinel host invalid {" + x + "} must match host:port ");
                                    })
                            .collect(Collectors.toSet());
        }
    }
}
