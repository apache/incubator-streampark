package com.streamxhub.console.base.function;

import com.streamxhub.console.base.exception.RedisConnectException;

@FunctionalInterface
public interface JedisExecutor<T, R> {
    R excute(T t) throws RedisConnectException;
}
