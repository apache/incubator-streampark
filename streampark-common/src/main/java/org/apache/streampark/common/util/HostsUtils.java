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

package org.apache.streampark.common.util;

import io.netty.resolver.HostsFileParser;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class HostsUtils {

    private HostsUtils() {
    }

    public static Map<String, String> getSortSystemHosts() {
        Map<String, String> ipMap = new HashMap<>();
        try {
            HostsFileParser.parseSilently()
                .inet4Entries()
                .forEach((hostname, addr) -> ipMap.put(hostname, addr.getHostAddress()));
        } catch (Exception ignored) {
            // ignore
        }
        return ipMap.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public static Map<String, String> getSystemHosts() {
        return getSystemHosts(false);
    }
    public static Map<String, String> getSystemHosts(boolean excludeLocalHost) {
        Map<String, String> map = new HashMap<>();
        try {
            HostsFileParser.parseSilently()
                .inet4Entries()
                .forEach((hostname, addr) -> map.put(hostname, addr.getHostAddress()));
        } catch (Exception ignored) {
            // ignore
        }
        if (excludeLocalHost) {
            try {
                String localHostName = InetAddress.getLocalHost().getHostName();
                map.entrySet().removeIf(e -> "localhost".equals(e.getKey()) || localHostName.equals(e.getKey())
                    || "127.0.0.1".equals(e.getValue()));
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    public static Map<String, String> getSystemHostsAsJava(boolean excludeLocalHost) {
        return getSystemHosts(excludeLocalHost);
    }
}
