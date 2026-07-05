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

package org.apache.streampark.flink.connector.elasticsearch5.conf;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ESConfig implements Serializable {
    public final ESSinkConfigOption sinkOption;
    public final boolean disableFlushOnCheckpoint;
    public final List<InetSocketAddress> host;

    public ESConfig(Properties parameters) {
        this.sinkOption = ESSinkConfigOption.of(parameters);
        this.disableFlushOnCheckpoint = sinkOption.disableFlushOnCheckpoint.get();
        this.host = Arrays.stream(sinkOption.host.get()).collect(Collectors.toList());
    }
}
