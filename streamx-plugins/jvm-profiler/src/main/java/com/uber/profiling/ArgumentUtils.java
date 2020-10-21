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

package com.uber.profiling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArgumentUtils {

    public static boolean needToUpdateArg(String argValue) {
        return argValue != null && !argValue.isEmpty();
    }

    public static String getArgumentSingleValue(Map<String, List<String>> parsedArgs, String argName) {
        List<String> list = parsedArgs.get(argName);
        if (list == null) {
            return null;
        }

        if (list.isEmpty()) {
            return "";
        }

        return list.get(list.size() - 1);
    }

    public static List<String> getArgumentMultiValues(Map<String, List<String>> parsedArgs, String argName) {
        List<String> list = parsedArgs.get(argName);
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }
}
