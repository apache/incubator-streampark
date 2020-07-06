package com.streamxhub.flink.core.function;


import java.io.Serializable;

@FunctionalInterface
public interface SQLFunction extends Serializable {
    String getSQL() throws Exception;
}
