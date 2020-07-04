package com.streamxhub.flink.core;


import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment;
import scala.Function2;
import scala.runtime.BoxedUnit;

import static com.streamxhub.common.conf.ConfigConst.*;

public class StreamingJavaContext extends StreamingContext {

    public StreamingJavaContext(FlinkInitializer initializer, Function2<StreamExecutionEnvironment, ParameterTool, BoxedUnit> fun) {
        super(initializer.parameter(), initializer.initStreamEnv(null));
    }

    public JobExecutionResult start() {
        System.out.println("\033[95;1m" + LOGO() + "\033[1m\n");
        String appName = this.parameter().get(KEY_FLINK_APP_NAME(), "");
        System.out.println("[StreamX] FlinkStreaming " + appName + " Starting...");
        return this.execute(appName);
    }
}