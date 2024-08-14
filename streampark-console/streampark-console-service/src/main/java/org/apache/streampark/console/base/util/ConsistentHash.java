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

package org.apache.streampark.console.base.util;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {

    // the number of virtual nodes for each server
    private final int numberOfReplicas = 2 << 16;

    // the hash ring of servers
    private final SortedMap<Long, T> circle = new TreeMap<>();

    /**
     * Initialize the ConsistentHash with a collection of servers.
     * @param servers the collection of servers
     */
    public ConsistentHash(Collection<T> servers) {
        servers.forEach(this::add);
    }

    /**
     * Add the virtual nodes of the server to the hash ring.
     * @param server the server to be added
     */
    public void add(T server) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(Murmur3Hash.hash64(server.toString() + i), server);
        }
    }

    /**
     * Remove the virtual nodes of the server from the hash ring.
     * @param server the server to be removed
     */
    public void remove(T server) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(Murmur3Hash.hash64(server.toString() + i));
        }
    }

    /**
     * Get the server that the key belongs to from the hash ring.
     * @param key the key
     * @return the specified server
     */
    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = Murmur3Hash.hash64(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    /**
     * Get the size of the hash ring.
     * @return the size of the hash ring
     */
    public long getSize() {
        return circle.size();
    }

}
