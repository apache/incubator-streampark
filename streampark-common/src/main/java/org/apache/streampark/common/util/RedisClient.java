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

package org.apache.streampark.common.util;

import org.apache.streampark.common.constants.Constants;

import org.apache.streampark.shaded.org.slf4j.Logger;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Redis connection pool and cluster client factory. */
public final class RedisClient {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(RedisClient.class.getName());

    private static final ConcurrentHashMap<RedisEndpoint, JedisPool> POOLS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<RedisEndpoint, JedisCluster> CLUSTERS = new ConcurrentHashMap<>();

    private static final JedisPoolConfig POOL_CONFIG;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(1000);
        poolConfig.setMaxIdle(64);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(false);
        poolConfig.setMinEvictableIdleTimeMillis(1800000);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        poolConfig.setNumTestsPerEvictionRun(-1);
        POOL_CONFIG = poolConfig;
    }

    private RedisClient() {
    }

    public static Jedis connect(RedisEndpoint[] endpoints) {
        if (endpoints.length == 0) {
            throw new IllegalArgumentException("[StreamPark] The RedisEndpoint array is empty!!!");
        }
        int index = ThreadLocalRandom.current().nextInt(endpoints.length);
        try {
            return connect(endpoints[index]);
        } catch (Exception e) {
            LOG.error("[StreamPark] {}", e.getMessage());
            RedisEndpoint[] remaining = new RedisEndpoint[endpoints.length - index - 1];
            System.arraycopy(endpoints, index + 1, remaining, 0, remaining.length);
            if (remaining.length == 0) {
                throw e;
            }
            return connect(remaining);
        }
    }

    public static Jedis connect(RedisEndpoint endpoint) {
        JedisPool pool = POOLS.computeIfAbsent(endpoint, RedisClient::createJedisPool);
        int sleepTime = 4;
        Jedis conn = null;
        while (conn == null) {
            try {
                conn = pool.getResource();
            } catch (JedisConnectionException e) {
                if (e.getCause() != null
                    && e.getCause().toString().contains("ERR max number of clients reached")) {
                    if (sleepTime < 500) {
                        sleepTime *= 2;
                    }
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        return conn;
    }

    public static JedisPool createJedisPool(RedisEndpoint endpoint) {
        RedisEndpoint endpointEn = endpoint.withAuth(Constants.DEFAULT_DATAMASK_STRING);
        LOG.info("[StreamPark] RedisClient: createJedisPool with {}", endpointEn);
        return new JedisPool(
            POOL_CONFIG,
            endpoint.getHost(),
            endpoint.getPort(),
            endpoint.getTimeout(),
            endpoint.getAuth(),
            endpoint.getDb());
    }

    public static JedisCluster connectCluster(RedisEndpoint... endpoints) {
        if (endpoints.length == 0) {
            throw new IllegalArgumentException("[StreamPark] The RedisEndpoint array is empty!!!");
        }
        RedisEndpoint head = endpoints[0];
        return CLUSTERS.computeIfAbsent(
            head,
            key -> {
                Set<HostAndPort> hostPorts = new HashSet<>();
                for (RedisEndpoint r : endpoints) {
                    hostPorts.add(new HostAndPort(r.getHost(), r.getPort()));
                }
                return new JedisCluster(hostPorts, head.getTimeout(), 1000, 1, head.getAuth(), POOL_CONFIG);
            });
    }

    public static void close() {
        for (Map.Entry<RedisEndpoint, JedisPool> entry : POOLS.entrySet()) {
            entry.getValue().close();
        }
    }
}
