package com.uber.profiling.reporters;

import com.uber.profiling.Reporter;
import com.uber.profiling.util.AgentLogger;
import com.uber.profiling.util.JsonUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisOutputReporter implements Reporter {

    private static final AgentLogger logger = AgentLogger.getLogger(RedisOutputReporter.class.getName());
    private JedisPool redisConn = null;

    @Override
    public void updateArguments(Map<String, List<String>> parsedArgs) {
    }

    //JedisPool should always be used as it is thread safe.
    @Override
    public void report(String profilerName, Map<String, Object> metrics) {
        ensureJedisConn();
        try {
            Jedis jedisClient = redisConn.getResource();
            jedisClient.set(createOriginStamp(profilerName), JsonUtils.serialize(metrics));
            redisConn.returnResource(jedisClient);
        } catch (Exception err) {
            logger.warn(err.toString());
        }
    }

    public String createOriginStamp(String profilerName) {
        try {
            return (profilerName + "-" + InetAddress.getLocalHost().getHostAddress() + "-" + System.currentTimeMillis());
        } catch (UnknownHostException err) {
            logger.warn("Address could not be determined and will be omitted!");
            return (profilerName + "-" + System.currentTimeMillis());
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            redisConn.close();
            redisConn = null;
        }
    }

    private void ensureJedisConn() {
        synchronized (this) {
            if (redisConn == null || redisConn.isClosed()) {
                redisConn = new JedisPool(System.getenv("JEDIS_PROFILER_CONNECTION"));
                return;
            }
        }
    }
}
