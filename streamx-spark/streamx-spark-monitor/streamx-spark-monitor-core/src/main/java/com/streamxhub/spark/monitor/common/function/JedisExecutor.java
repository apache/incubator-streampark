package com.streamxhub.spark.monitor.common.function;

import com.streamxhub.spark.monitor.common.exception.RedisConnectException;

@FunctionalInterface
public interface JedisExecutor<T, R> {
    R excute(T t) throws RedisConnectException;
}
