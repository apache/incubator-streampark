package com.streamxhub.console.test;

import com.streamxhub.flink.submit.FlinkSubmit;
import com.streamxhub.flink.submit.SubmitInfo;

public class FlinkSubmitTest {

    public static void main(String[] args) {
        SubmitInfo submit = new SubmitInfo(
                "nameservice1",
                "hdfs://nameservice1/streamx/workspace/1308238434145062914/flink-quickstart-1.0/flink-quickstart-1.0.jar",
                "BatchJob",
                "json://{\"flink.deployment.option.class\":\"org.apache.flink.learn.BatchJob\"}",
                "Apache Flink",
                null,
                "-yjm 2048 -ytm 2048 -p 3 -ys 3".split("\\s+"),
                new String[0],
                null
        );
        FlinkSubmit.submit(submit);
    }
}
