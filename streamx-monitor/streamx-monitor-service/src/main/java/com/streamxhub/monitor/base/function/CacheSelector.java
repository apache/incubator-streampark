package com.streamxhub.monitor.base.function;

@FunctionalInterface
public interface CacheSelector<T> {
    T select() throws Exception;
}
