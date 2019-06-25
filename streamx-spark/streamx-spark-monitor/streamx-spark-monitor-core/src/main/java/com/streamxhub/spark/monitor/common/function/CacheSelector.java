package com.streamxhub.spark.monitor.common.function;

@FunctionalInterface
public interface CacheSelector<T> {
    T select() throws Exception;
}
