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

package org.apache.streampark.console.core.registry;

import org.apache.streampark.common.lifecycle.ServerLifeCycleManager;
import org.apache.streampark.registry.api.ConnectionListener;
import org.apache.streampark.registry.api.ConnectionState;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleConnectionStateListener implements ConnectionListener {

    private final ConsoleConnectStrategy consoleConnectStrategy;

    public ConsoleConnectionStateListener(@NonNull ConsoleConnectStrategy consoleConnectStrategy) {
        this.consoleConnectStrategy = consoleConnectStrategy;
    }

    @Override
    public void onUpdate(ConnectionState state) {
        log.info("Master received a {} event from registry, the current server state is {}", state,
            ServerLifeCycleManager.getServerStatus());
        switch (state) {
            case CONNECTED:
                break;
            case SUSPENDED:
                break;
            case RECONNECTED:
                consoleConnectStrategy.reconnect();
                break;
            case DISCONNECTED:
                consoleConnectStrategy.disconnect();
                break;
            default:
        }
    }
}
