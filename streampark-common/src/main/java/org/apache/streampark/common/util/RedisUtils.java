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

import org.apache.streampark.shaded.org.slf4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Redis command utilities. */
public final class RedisUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(RedisUtils.class.getName());

    private RedisUtils() {
    }

    public static boolean exists(String key, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.exists(key), endpoint);
    }

    public static boolean hexists(String key, String field, RedisEndpoint endpoint) {
        return hexists(key, field, null, endpoint);
    }

    public static boolean hexists(String key, String field, Runnable func, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.hexists(key, field), func, endpoint);
    }

    public static String get(String key, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.get(key), endpoint);
    }

    public static String hget(String key, String field, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.hget(key, field), endpoint);
    }

    public static String setex(String key, String value, Integer ttl, RedisEndpoint endpoint) {
        return setex(key, value, ttl, null, endpoint);
    }

    public static String setex(String key, String value, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.setex(key, ttl, value), func, endpoint);
    }

    public static Long setnx(String key, String value, Integer ttl, RedisEndpoint endpoint) {
        return setnx(key, value, ttl, null, endpoint);
    }

    public static Long setnx(String key, String value, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Long x = jedis.setnx(key, value);
                if (x == 1 && ttl != null) {
                    jedis.expire(key, ttl);
                }
                return x;
            },
            func,
            endpoint);
    }

    public static Long hsetnx(
                              String key, String field, String value, Integer ttl, RedisEndpoint endpoint) {
        return hsetnx(key, field, value, ttl, null, endpoint);
    }

    public static Long hsetnx(
                              String key, String field, String value, Integer ttl, Runnable func,
                              RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Long x = jedis.hsetnx(key, field, value);
                if (x == 1 && ttl != null) {
                    jedis.expire(key, ttl);
                }
                return x;
            },
            func,
            endpoint);
    }

    public static String[] mget(String[] keys, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.mget(keys).toArray(new String[0]), endpoint);
    }

    public static Long del(String key, RedisEndpoint endpoint) {
        return del(key, null, endpoint);
    }

    public static Long del(String key, Runnable func, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.del(key), func, endpoint);
    }

    public static String set(String key, String value, Integer ttl, RedisEndpoint endpoint) {
        return set(key, value, ttl, null, endpoint);
    }

    public static String set(String key, String value, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                String s = jedis.set(key, value);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return s;
            },
            func,
            endpoint);
    }

    public static Long hset(String key, String field, String value, Integer ttl, RedisEndpoint endpoint) {
        return hset(key, field, value, ttl, null, endpoint);
    }

    public static Long hset(
                            String key, String field, String value, Integer ttl, Runnable func,
                            RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Long s = jedis.hset(key, field, value);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return s;
            },
            func,
            endpoint);
    }

    public static String hmset(
                               String key, Map<String, String> hash, Integer ttl, RedisEndpoint endpoint) {
        return hmset(key, hash, ttl, null, endpoint);
    }

    public static String hmset(
                               String key, Map<String, String> hash, Integer ttl, Runnable func,
                               RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                String s = jedis.hmset(key, hash);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return s;
            },
            func,
            endpoint);
    }

    public static List<String> hmget(String key, String[] fields, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.hmget(key, fields), endpoint);
    }

    public static Map<String, String> hgetAll(String key, RedisEndpoint endpoint) {
        return doRedis(jedis -> new HashMap<>(jedis.hgetAll(key)), endpoint);
    }

    public static Long hdel(String key, List<String> fields, RedisEndpoint endpoint) {
        return hdel(key, fields, null, endpoint);
    }

    public static Long hdel(String key, List<String> fields, Runnable func, RedisEndpoint endpoint) {
        if (key == null || fields == null || fields.isEmpty()) {
            return 0L;
        }
        return doRedis(jedis -> jedis.hdel(key, fields.toArray(new String[0])), func, endpoint);
    }

    public static Long sadd(String key, List<String> members, Integer ttl, RedisEndpoint endpoint) {
        return sadd(key, members, ttl, null, endpoint);
    }

    public static Long sadd(
                            String key, List<String> members, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Long res = jedis.sadd(key, members.toArray(new String[0]));
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return res;
            },
            func,
            endpoint);
    }

    public static Set<String> smembers(String key, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.smembers(key), endpoint);
    }

    public static Long srem(String key, List<String> members, RedisEndpoint endpoint) {
        return srem(key, members, null, endpoint);
    }

    public static Long srem(String key, List<String> members, Runnable func, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.srem(key, members.toArray(new String[0])), func, endpoint);
    }

    public static String getOrElseHset(
                                       String key, String field, String value, Integer ttl, RedisEndpoint endpoint) {
        return getOrElseHset(key, field, value, ttl, null, endpoint);
    }

    public static String getOrElseHset(
                                       String key,
                                       String field,
                                       String value,
                                       Integer ttl,
                                       Runnable func,
                                       RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                String v = jedis.hget(key, field);
                if (v == null) {
                    jedis.hset(key, field, value);
                    if (ttl != null) {
                        jedis.expire(key, ttl);
                    }
                }
                return v;
            },
            func,
            endpoint);
    }

    public static String getOrElseSet(String key, String value, Integer ttl, RedisEndpoint endpoint) {
        return getOrElseSet(key, value, ttl, null, endpoint);
    }

    public static String getOrElseSet(
                                      String key, String value, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                String v = jedis.get(key);
                if (v == null) {
                    jedis.set(key, value);
                    if (ttl != null) {
                        jedis.expire(key, ttl);
                    }
                }
                return v;
            },
            func,
            endpoint);
    }

    public static Long hincrBy(
                               String key, String field, long value, Integer ttl, RedisEndpoint endpoint) {
        return hincrBy(key, field, value, ttl, null, endpoint);
    }

    public static Long hincrBy(
                               String key, String field, long value, Integer ttl, Runnable func,
                               RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Long reply = jedis.hincrBy(key, field, value);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return reply;
            },
            func,
            endpoint);
    }

    public static Double hincrByFloat(
                                      String key, String field, double value, Integer ttl, RedisEndpoint endpoint) {
        return hincrByFloat(key, field, value, ttl, null, endpoint);
    }

    public static Double hincrByFloat(
                                      String key, String field, double value, Integer ttl, Runnable func,
                                      RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Double reply = jedis.hincrByFloat(key, field, value);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return reply;
            },
            func,
            endpoint);
    }

    public static Long incrBy(String key, long value, Integer ttl, RedisEndpoint endpoint) {
        return incrBy(key, value, ttl, null, endpoint);
    }

    public static Long incrBy(
                              String key, long value, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Long reply = jedis.incrBy(key, value);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return reply;
            },
            func,
            endpoint);
    }

    public static Double incrByFloat(String key, double value, Integer ttl, RedisEndpoint endpoint) {
        return incrByFloat(key, value, ttl, null, endpoint);
    }

    public static Double incrByFloat(
                                     String key, double value, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                Double reply = jedis.incrByFloat(key, value);
                if (ttl != null) {
                    jedis.expire(key, ttl);
                }
                return reply;
            },
            func,
            endpoint);
    }

    public static Long mSets(List<Map.Entry<String, String>> kvs, Integer ttl, RedisEndpoint endpoint) {
        return mSets(kvs, ttl, null, endpoint);
    }

    public static Long mSets(
                             List<Map.Entry<String, String>> kvs, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                long start = System.currentTimeMillis();
                Pipeline pipe = jedis.pipelined();
                for (Map.Entry<String, String> kv : kvs) {
                    pipe.mset(kv.getKey(), kv.getValue());
                    if (ttl != null) {
                        pipe.expire(kv.getKey(), ttl);
                    }
                }
                pipe.sync();
                return System.currentTimeMillis() - start;
            },
            func,
            endpoint);
    }

    public static Long mSetex(List<Map.Entry<String, String>> kvs, Integer ttl, RedisEndpoint endpoint) {
        return mSetex(kvs, ttl, null, endpoint);
    }

    public static Long mSetex(
                              List<Map.Entry<String, String>> kvs, Integer ttl, Runnable func, RedisEndpoint endpoint) {
        return doRedis(
            jedis -> {
                long start = System.currentTimeMillis();
                Pipeline pipe = jedis.pipelined();
                for (Map.Entry<String, String> kv : kvs) {
                    pipe.setnx(kv.getKey(), kv.getValue());
                    if (ttl != null) {
                        pipe.expire(kv.getKey(), ttl);
                    }
                }
                pipe.sync();
                return System.currentTimeMillis() - start;
            },
            func,
            endpoint);
    }

    public static Long expire(String key, int seconds, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.expire(key, seconds), endpoint);
    }

    public static void delByPattern(String key, RedisEndpoint endpoint) {
        delByPattern(key, null, endpoint);
    }

    public static void delByPattern(String key, Runnable func, RedisEndpoint endpoint) {
        doRedis(
            jedis -> {
                ScanParams scanParams = new ScanParams();
                scanParams.match(key);
                scanParams.count(10000);
                String cursor = ScanParams.SCAN_POINTER_START;
                do {
                    redis.clients.jedis.ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                    cursor = scanResult.getCursor();
                    List<String> keys = scanResult.getResult();
                    if (!keys.isEmpty()) {
                        jedis.del(keys.toArray(new String[0]));
                    }
                } while (!"0".equals(cursor));
                return null;
            },
            func,
            endpoint);
    }

    public static Long hlen(String key, RedisEndpoint endpoint) {
        return doRedis(jedis -> jedis.hlen(key), endpoint);
    }

    public static <R> R doRedis(Function<Jedis, R> f, RedisEndpoint endpoint) {
        return doRedis(f, null, endpoint);
    }

    public static <R> R doRedis(Function<Jedis, R> f, Runnable func, RedisEndpoint endpoint) {
        Jedis redis = RedisClient.connect(endpoint);
        R result;
        if (func == null) {
            result = f.apply(redis);
        } else {
            Transaction transaction = redis.multi();
            result = f.apply(redis);
            func.run();
            transaction.exec();
            transaction.close();
        }
        try {
            redis.close();
            LOG.debug("[StreamPark] jedis.close successful.");
        } catch (Exception e) {
            LOG.error("[StreamPark] jedis.close failed.");
        }
        return result;
    }

    public static <R> R doCluster(Function<JedisCluster, R> f, RedisEndpoint... endpoints) {
        JedisCluster cluster = RedisClient.connectCluster(endpoints);
        R result = f.apply(cluster);
        try {
            cluster.close();
            LOG.debug("[StreamPark] cluster.close successful.");
        } catch (Exception e) {
            LOG.error("[StreamPark] cluster.close failed.");
        }
        return result;
    }

    public static <R> R doPipeline(Function<Pipeline, R> f, RedisEndpoint endpoint) {
        Jedis redis = RedisClient.connect(endpoint);
        Pipeline pipe = redis.pipelined();
        R result = f.apply(pipe);
        try {
            pipe.sync();
            pipe.close();
            redis.close();
            LOG.debug("[StreamPark] pipe.close successful.");
        } catch (Exception e) {
            LOG.error("[StreamPark] pipe.close failed.");
        }
        return result;
    }
}
