/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.common.conf;

/**
 * @author benjobs
 */
public class ConfigurationOptions {


    public static String KEY_APPLICATION_ARGS = "$internal.application.program-args";

    public static String KEY_APPLICATION_MAIN_CLASS = "$internal.application.main";

    public static String KEY_TOTAL_PROCESS_MEMORY = "jobmanager.memory.process.size";

    public static String KEY_TOTAL_FLINK_MEMORY = "jobmanager.memory.flink.size";

    public static String KEY_JVM_HEAP_MEMORY = "jobmanager.memory.heap.size";

    public static String KEY_OFF_HEAP_MEMORY = "jobmanager.memory.off-heap.size";
    /**
     * ref KubernetesConfigOptions.NAMESPACE.defaultValue()
     */
    public static String KUBERNETES_NAMESPACE_DEFAULT_VALUE = "default";

}
