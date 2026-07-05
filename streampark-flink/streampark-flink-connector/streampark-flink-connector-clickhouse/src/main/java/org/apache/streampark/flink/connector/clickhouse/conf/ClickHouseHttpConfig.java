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

package org.apache.streampark.flink.connector.clickhouse.conf;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.flink.connector.conf.ThresholdConf;
import java.util.Base64; import java.util.List; import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
public class ClickHouseHttpConfig extends ThresholdConf {
    public final ClickHouseSinkConfigOption sinkOption;
    public final String user, password, credentials;
    public final List<String> hosts;
    private int currentHostId = 0;
    public ClickHouseHttpConfig(Properties parameters) {
        super(ClickHouseSinkConfigOption.CLICKHOUSE_SINK_PREFIX, parameters);
        sinkOption = ClickHouseSinkConfigOption.of(parameters);
        user = sinkOption.user.get(); password = sinkOption.password.get();
        hosts = sinkOption.hosts.get();
        if (user == null && password == null) credentials = null;
        else credentials = Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }
    public String getRandomHostUrl() { currentHostId = ThreadLocalRandom.current().nextInt(hosts.size()); return hosts.get(currentHostId); }
    public String nextHost() { currentHostId = currentHostId >= hosts.size() - 1 ? 0 : currentHostId + 1; return hosts.get(currentHostId); }
    @Override public String toString() { return String.format("{ user: %s, password: %s, hosts: %s }", user, Constants.DEFAULT_DATAMASK_STRING, String.join(",", hosts)); }
}
