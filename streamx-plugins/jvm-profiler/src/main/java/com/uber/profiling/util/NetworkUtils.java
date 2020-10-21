/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.util;

import java.net.InetAddress;
import java.util.Map;

public class NetworkUtils {
    public static String getLocalHostName() {
        try {
            Map<String, String> env = System.getenv();
            if (env.containsKey("COMPUTERNAME"))
                return env.get("COMPUTERNAME");
            else if (env.containsKey("HOSTNAME"))
                return env.get("HOSTNAME");
            else
                return InetAddress.getLocalHost().getHostName();
        } catch (Throwable e) {
            return "unknown_localhost_name";
        }
    }

    public static void main(String[] args) {
        System.out.println(getLocalHostName());
    }
}
