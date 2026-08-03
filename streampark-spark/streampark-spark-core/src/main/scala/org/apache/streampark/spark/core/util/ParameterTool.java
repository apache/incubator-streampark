/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.spark.core.util;

import org.apache.commons.lang3.math.NumberUtils;
import java.util.*;


public class ParameterTool  {

    protected static final String NO_VALUE_KEY = "__NO_VALUE_KEY";

    public static String getKeyFromArgs(String[] args, int index) {
        String key;
        if (args[index].startsWith("--")) {
            key = args[index].substring(2);
        } else if (args[index].startsWith("-")) {
            key = args[index].substring(1);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Error parsing arguments '%s' on '%s'. Please prefix keys with -- or -.",
                            Arrays.toString(args), args[index]));
        }

        if (key.isEmpty()) {
            throw new IllegalArgumentException(
                    "The input " + Arrays.toString(args) + " contains an empty argument");
        }

        return key;
    }

    public static Map<String, String> fromArgs(String[] args) {
        final Map<String, String> map = new HashMap<>(args.length / 2);

        int i = 0;
        while (i < args.length) {
            final String key = getKeyFromArgs(args, i);

            if (key.isEmpty()) {
                throw new IllegalArgumentException(
                        "The input " + Arrays.toString(args) + " contains an empty argument");
            }

            i += 1; // try to find the value

            if (i >= args.length) {
                map.put(key, NO_VALUE_KEY);
            } else if (NumberUtils.isCreatable(args[i])) {
                map.put(key, args[i]);
                i += 1;
            } else if (args[i].startsWith("--") || args[i].startsWith("-")) {
                // the argument cannot be a negative number because we checked earlier
                // -> the next argument is a parameter name
                map.put(key, NO_VALUE_KEY);
            } else {
                map.put(key, args[i]);
                i += 1;
            }
        }

        return map;
    }
}
