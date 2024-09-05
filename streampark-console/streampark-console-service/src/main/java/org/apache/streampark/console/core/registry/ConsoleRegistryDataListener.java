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

import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.console.base.util.SpringContextUtils;
import org.apache.streampark.registry.api.Event;
import org.apache.streampark.registry.api.SubscribeListener;
import org.apache.streampark.registry.api.enums.RegistryNodeType;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleRegistryDataListener implements SubscribeListener {

    private final ConsoleRegistryClient consoleRegistryClient;

    public ConsoleRegistryDataListener() {
        consoleRegistryClient = SpringContextUtils.getBean(ConsoleRegistryClient.class);
    }

    @Override
    public void notify(Event event) {
        final String path = event.path();
        if (Strings.isNullOrEmpty(path)) {
            return;
        }
        // monitor console
        if (path.startsWith(RegistryNodeType.CONSOLE_SERVER.getRegistryPath() + Constants.SINGLE_SLASH)) {
            handleConsoleEvent(event);
        }
    }

    private void handleConsoleEvent(Event event) {
        final String path = event.path();
        switch (event.type()) {
            case ADD:
                log.info("console node added : {}", path);
                break;
            case REMOVE:
                log.info("console node deleted : {}", path);
                consoleRegistryClient.removeConsoleNodePath(path, RegistryNodeType.CONSOLE_SERVER, true);
                break;
            default:
                break;
        }
    }

}
