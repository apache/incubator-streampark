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

package org.apache.streampark.flink.connector.redis.bean;

import org.apache.streampark.common.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Redis two-phase commit transaction buffer. */
public class RedisTransaction<T> implements Serializable {

    public final String transactionId;
    public final List<Entry<T>> mapper;
    public boolean invoked;

    public RedisTransaction() {
        this(Utils.uuid(), new ArrayList<>(), false);
    }

    public RedisTransaction(String transactionId, List<Entry<T>> mapper, boolean invoked) {
        this.transactionId = transactionId;
        this.mapper = mapper;
        this.invoked = invoked;
    }

    public void add(RedisMapper<T> redisMapper, T value, int ttl) {
        mapper.add(new Entry<>(redisMapper, value, ttl));
    }

    @Override
    public String toString() {
        return "(transactionId:" + transactionId + ",size:" + mapper.size() + ",invoked:" + invoked + ")";
    }

    public static class Entry<T> implements Serializable {
        public final RedisMapper<T> mapper;
        public final T value;
        public final int ttl;

        public Entry(RedisMapper<T> mapper, T value, int ttl) {
            this.mapper = mapper;
            this.value = value;
            this.ttl = ttl;
        }
    }
}
