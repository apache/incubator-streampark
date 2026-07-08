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

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class SparkConfigurationUtils {

    private static final Pattern SPARK_PROPERTY_COMPLEX_PATTERN =
        Pattern.compile("^[\"']?(.*?)=(.*?)[\"']?$");
    private static final String SPARK_ARGUMENT_REGEXP =
        "\"?(\\s++|$)(?=(([^\"]*\"){2})*+[^\"]*$)\"?";

    private SparkConfigurationUtils() {
    }

    @Nonnull
    public static Map<String, String> extractPropertiesAsJava(String properties) {
        return new HashMap<>(extractProperties(properties));
    }

    @Nonnull
    public static Map<String, String> extractProperties(String properties) {
        if (StringUtils.isEmpty(properties)) {
            return new HashMap<>();
        }
        Map<String, String> map = new HashMap<>();
        for (String x : properties.split("(\\s)*+(--conf|-c)(\\s)++")) {
            if (Utils.isNotEmpty(x) && !x.isEmpty()) {
                java.util.regex.Matcher p = SPARK_PROPERTY_COMPLEX_PATTERN.matcher(x);
                if (p.matches()) {
                    map.put(p.group(1).trim(), p.group(2).trim());
                }
            }
        }
        return map;
    }

    @Nonnull
    public static List<String> extractArgumentsAsJava(String arguments) {
        if (StringUtils.isEmpty(arguments)) {
            return Lists.newArrayList();
        }
        List<String> result = new ArrayList<>();
        for (String s : arguments.split(SPARK_ARGUMENT_REGEXP)) {
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }
}
