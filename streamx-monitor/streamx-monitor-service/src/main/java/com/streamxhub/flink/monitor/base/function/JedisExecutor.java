package com.streamxhub.flink.monitor.base.function;

import com.streamxhub.flink.monitor.base.exception.RedisConnectException;

@FunctionalInterface
public interface JedisExecutor<T, R> {
    R excute(T t) throws RedisConnectException;
}
