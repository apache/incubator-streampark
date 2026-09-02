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

package org.apache.streampark.flink.cli;

import java.lang.reflect.Method;

/** Loads Flink {@code ParameterTool} across 1.x and 2.x package relocations. */
final class FlinkParameterToolBridge {

    private static final String[] PARAMETER_TOOL_CLASSES =
        new String[]{
                "org.apache.flink.util.ParameterTool",
                "org.apache.flink.api.java.utils.ParameterTool"
        };

    private final Object delegate;

    private FlinkParameterToolBridge(Object delegate) {
        this.delegate = delegate;
    }

    static FlinkParameterToolBridge fromArgs(String[] args) {
        for (String className : PARAMETER_TOOL_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                Method fromArgs = clazz.getMethod("fromArgs", String[].class);
                return new FlinkParameterToolBridge(fromArgs.invoke(null, (Object) args));
            } catch (ClassNotFoundException ignored) {
                // try next package
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to parse program arguments", e);
            }
        }
        throw new IllegalStateException("ParameterTool not found on classpath");
    }

    String get(String key) {
        return get(key, null);
    }

    String get(String key, String defaultValue) {
        try {
            Method get = delegate.getClass().getMethod("get", String.class, String.class);
            return (String) get.invoke(delegate, key, defaultValue);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read parameter: " + key, e);
        }
    }
}
