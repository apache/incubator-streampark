/*
 * Copyright (c) 2019 The StreamX Project
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

package com.streamxhub.plugin.profiling;

import java.lang.instrument.Instrumentation;

/**
 * @author benjobs
 */
public final class Agent {

    private static AgentImpl agentImpl = new AgentImpl();

    private Agent() {
    }

    public static void agentmain(final String args, final Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    public static void premain(final String args, final Instrumentation instrumentation) {
        System.out.println("Java Agent " + AgentImpl.VERSION + " premain args: " + args);
        Arguments arguments = Arguments.parseArgs(args);
        arguments.runConfigProvider();
        agentImpl.run(arguments, instrumentation, null);
    }
}
