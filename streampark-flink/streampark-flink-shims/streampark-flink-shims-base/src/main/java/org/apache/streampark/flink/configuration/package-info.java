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

/**
 * Flink-specific configuration declarations, runtime views, and submission adapters.
 *
 * <p>StreamPark application options and native Flink configuration remain separate until the
 * initializer creates a {@link org.apache.streampark.flink.configuration.FlinkRuntimeConfiguration}
 * for a concrete job. {@link FlinkConfigurationUtils}
 * normalizes Flink's legacy and standard YAML file formats before those views are assembled.
 *
 * <p>Installations before Flink 1.19 use flat {@code flink-conf.yaml}. Flink 1.19 and 1.20 prefer
 * that legacy file when present and otherwise use standard {@code config.yaml}. Flink 2.0 and
 * later require {@code config.yaml}. The selected format is carried with compressed configuration
 * content because a runtime payload no longer retains its source filename.
 */
package org.apache.streampark.flink.configuration;

import org.apache.streampark.common.util.FlinkConfigurationUtils;
