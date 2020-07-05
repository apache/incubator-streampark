package com.streamxhub.flink.javacase;

import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.source.KafakJavaSource;
import com.streamxhub.flink.core.source.KafkaRecord;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaSourceApp {

    public static void main(String[] args) {

        StreamingContext context = new StreamingContext(args,null);

        new KafakJavaSource<LogBean>(context)
                .deserializer(new KafkaDeserializationSchema<LogBean>() {
                    @Override
                    public TypeInformation<LogBean> getProducedType() {
                        return TypeInformation.of(LogBean.class);
                    }

                    @Override
                    public boolean isEndOfStream(LogBean nextElement) {
                        return false;
                    }

                    @Override
                    public LogBean deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
                        String value = new String(record.value());
                        LogBean logBean = new LogBean();
                        //value to logBean....
                        return logBean;
                    }
                }).getDataStream()
                .map(new MapFunction<KafkaRecord<LogBean>, String>() {
                    @Override
                    public String map(KafkaRecord<LogBean> value) throws Exception {
                        return value.value().getControlid();
                    }
                }).print();

        context.execute();
    }


}
