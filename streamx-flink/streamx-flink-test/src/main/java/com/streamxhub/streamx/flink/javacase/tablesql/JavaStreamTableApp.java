package com.streamxhub.streamx.flink.javacase.tablesql;

import com.streamxhub.streamx.flink.core.scala.StreamTableContext;
import com.streamxhub.streamx.flink.core.scala.util.StreamTableEnvConfig;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.scala.DataStream;
import org.apache.flink.table.api.Table;

import java.time.ZoneId;
import java.util.Arrays;

public class JavaStreamTableApp {

    public static void main(String[] args) {
        StreamTableEnvConfig javaConfig = new StreamTableEnvConfig(args, (environment, parameterTool) -> {
            environment.getConfig().enableForceAvro();
        }, (tableConfig, parameterTool) -> {
            tableConfig.setLocalTimeZone(ZoneId.of("Asia/Shanghai"));
        });

        StreamTableContext context = new StreamTableContext(javaConfig);

        SingleOutputStreamOperator<JavaEntity> source = context.getJavaEnv().fromCollection(
                Arrays.asList(
                        "flink,apapche flink",
                        "kafka,apapche kafka",
                        "spark,spark",
                        "zookeeper,apapche zookeeper",
                        "hadoop,apapche hadoop"
                )
        ).map((MapFunction<String, JavaEntity>) JavaEntity::new);

        context.createTemporaryView("mySource", new DataStream<>(source));

        Table table = context.from("mySource");
        context.toAppendStream(table, TypeInformation.of(JavaEntity.class)).print();

        context.start("Flink SQl Job");
    }

    public static class JavaEntity {
        public String id;
        public String name;

        public JavaEntity() {
        }

        public JavaEntity(String str) {
            String[] array = str.split(",");
            this.id = array[0];
            this.name = array[1];
        }
    }

}




