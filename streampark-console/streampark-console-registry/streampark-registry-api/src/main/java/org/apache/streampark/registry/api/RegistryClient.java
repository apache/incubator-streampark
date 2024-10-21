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

package org.apache.streampark.registry.api;

import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.registry.api.enums.RegistryNodeType;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Component
@Slf4j
public class RegistryClient {

    private static final String EMPTY = "";

    private final Registry registry;

    public RegistryClient(Registry registry) {
        this.registry = registry;
        // TODO: remove this
        if (!registry.exists(RegistryNodeType.CONSOLE_SERVER.getRegistryPath())) {
            registry.put(RegistryNodeType.CONSOLE_SERVER.getRegistryPath(), EMPTY);
        }
    }

    /**
     * get host ip:port, path format: parentPath/ip:port
     *
     * @param path path
     * @return host ip:port, string format: parentPath/ip:port
     */
    public String getHostByEventDataPath(String path) {
        checkArgument(!Strings.isNullOrEmpty(path), "path cannot be null or empty");

        final String[] pathArray = path.split(Constants.SINGLE_SLASH);

        checkArgument(pathArray.length >= 1, "cannot parse path: %s", path);

        return pathArray[pathArray.length - 1];
    }

    public void close() throws IOException {
        registry.close();
    }

    public void put(String key, String value) {
        registry.put(key, value);
    }

    public void delete(String key) {
        registry.delete(key);
    }

    public String get(String key) {
        return registry.get(key);
    }

    public void subscribe(String path, SubscribeListener listener) {
        registry.subscribe(path, listener);
    }

    public boolean exists(String key) {
        return registry.exists(key);
    }

    public Collection<String> getChildrenKeys(final String key) {
        return registry.children(key);
    }

    public Set<String> getServerNodeSet(RegistryNodeType nodeType) {
        try {
            return new HashSet<>(getServerNodes(nodeType));
        } catch (Exception e) {
            throw new RegistryException("Failed to get server node: " + nodeType, e);
        }
    }

    private Collection<String> getServerNodes(RegistryNodeType nodeType) {
        return getChildrenKeys(nodeType.getRegistryPath());
    }

    public boolean checkNodeExists(String host, RegistryNodeType nodeType) {
        return getServerNodes(nodeType)
            .stream()
            .anyMatch(it -> it.contains(host));
    }
}
