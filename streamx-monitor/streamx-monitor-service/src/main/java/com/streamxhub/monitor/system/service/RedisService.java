package com.streamxhub.monitor.system.service;

import com.streamxhub.monitor.base.domain.RedisInfo;
import com.streamxhub.monitor.base.exception.RedisConnectException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisService {

    /**
     * 获取 redis 的详细信息
     *
     * @return List
     */
    List<RedisInfo> getRedisInfo() throws RedisConnectException;

    /**
     * 获取 redis key 数量
     *
     * @return Map
     */
    Map<String, Object> getKeysSize() throws RedisConnectException;

    /**
     * 获取 redis 内存信息
     *
     * @return Map
     */
    Map<String, Object> getMemoryInfo() throws RedisConnectException;

    /**
     * 获取 key
     *
     * @param pattern 正则
     * @return Set
     */
    Set<String> getKeys(String pattern) throws RedisConnectException;

    /**
     * get命令
     *
     * @param key key
     * @return String
     */
    String get(String key) throws RedisConnectException;

    /**
     * set命令
     *
     * @param key   key
     * @param value value
     * @return String
     */
    String set(String key, String value) throws RedisConnectException;

    /**
     * set 命令
     *
     * @param key         key
     * @param value       value
     * @param milliscends 毫秒
     * @return String
     */
    String set(String key, String value, Long milliscends) throws RedisConnectException;

    /**
     * del命令
     *
     * @param key key
     * @return Long
     */
    Long del(String... key) throws RedisConnectException;

    /**
     * exists命令
     *
     * @param key key
     * @return Boolean
     */
    Boolean exists(String key) throws RedisConnectException;

    /**
     * pttl命令
     *
     * @param key key
     * @return Long
     */
    Long pttl(String key) throws RedisConnectException;

    /**
     * pexpire命令
     *
     * @param key         key
     * @param milliscends 毫秒
     * @return Long
     */
    Long pexpire(String key, Long milliscends) throws RedisConnectException;


    /**
     * zadd 命令
     *
     * @param key    key
     * @param score  score
     * @param member value
     */
    Long zadd(String key, Double score, String member) throws RedisConnectException;

    /**
     * zrangeByScore 命令
     *
     * @param key key
     * @param min min
     * @param max max
     * @return Set<String>
     */
    Set<String> zrangeByScore(String key, String min, String max) throws RedisConnectException;

    /**
     * zremrangeByScore 命令
     *
     * @param key   key
     * @param start start
     * @param end   end
     * @return Long
     */
    Long zremrangeByScore(String key, String start, String end) throws RedisConnectException;

    /**
     * zrem 命令
     *
     * @param key     key
     * @param members members
     * @return Long
     */
    Long zrem(String key, String... members) throws RedisConnectException;
}
