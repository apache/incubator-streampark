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

import java.io.Closeable;
import java.util.Collection;

/**
 * The SPI interface for registry center, each registry plugin should implement this interface.
 */
public interface Registry extends Closeable {

    /**
     * Start the registry, once started, the registry will connect to the registry center.
     */
    void start();

    /**
     * Subscribe the path, when the path has expose {@link Event}, the listener will be triggered.
     * <p>
     * The sub path will also be watched, if the sub path has event, the listener will be triggered.
     *
     * @param path     the path to subscribe
     * @param listener the listener to be triggered
     */
    void subscribe(String path, SubscribeListener listener);

    /**
     * Get the value of the key, if key not exist will throw {@link RegistryException}
     */
    String get(String key) throws RegistryException;

    /**
     * Put the key-value pair into the registry
     *
     * @param key                the key, cannot be null
     * @param value              the value, cannot be null
     */
    void put(String key, String value);

    /**
     * Delete the key from the registry
     */
    void delete(String key);

    /**
     * Return the children of the key
     */
    Collection<String> children(String key);

    /**
     * Check if the key exists
     *
     * @param key the key to check
     * @return true if the key exists
     */
    boolean exists(String key);
}
