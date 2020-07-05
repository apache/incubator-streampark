package com.streamxhub.flink.core.function;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@FunctionalInterface
public interface StreamEnvConfigFunction {
    /**
     * 用于初始化StreamExecutionEnvironment的时候,用于可以实现该函数,自定义要设置的参数...
     * @param environment
     * @param parameterTool
     */
    void envConfig(StreamExecutionEnvironment environment, ParameterTool parameterTool);
}
