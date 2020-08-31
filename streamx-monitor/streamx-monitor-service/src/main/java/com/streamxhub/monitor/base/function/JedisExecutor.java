package com.streamxhub.monitor.base.function;

import com.streamxhub.monitor.base.exception.RedisConnectException;

@FunctionalInterface
public interface JedisExecutor<T, R> {
    R excute(T t) throws RedisConnectException;
}
