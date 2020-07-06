package com.streamxhub.flink.core.function;


import java.io.Serializable;
import java.util.Map;

@FunctionalInterface
public interface ResultSetFunction<T> extends Serializable {
    T result(Map<String,?> map);
}

