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

package org.apache.streampark.common.configuration;

/**
 * Configuration sources in ascending precedence order.
 *
 * <p>The declaration order is part of the merge contract used by {@link Configuration.Builder}.
 * New sources must be placed according to their intended precedence; reordering existing constants
 * changes which value becomes effective.
 */
public enum ConfigSource {
    /** Built-in values supplied before external configuration is loaded. */
    DEFAULTS,

    /** Values parsed from configuration files. */
    FILE,

    /** Values obtained from process environment variables. */
    ENVIRONMENT,

    /** Values obtained from JVM system properties. */
    SYSTEM_PROPERTIES,

    /** Values supplied as application or launcher arguments. */
    COMMAND_LINE,

    /** Values set explicitly by a running StreamPark component. */
    RUNTIME
}
