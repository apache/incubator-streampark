package com.streamxhub.flink.core.source;

import com.streamxhub.flink.core.StreamingContext;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;

public class KafakJavaSource<T> {

    private StreamingContext ctx;
    private String[] topics;
    private String alias = "";
    private KafkaDeserializationSchema<T> deserializer;
    private AssignerWithPeriodicWatermarks<KafkaRecord<T>> assigner;

    public KafakJavaSource(StreamingContext ctx) {
        this.ctx = ctx;
        this.deserializer = (KafkaDeserializationSchema<T>) new KafkaStringDeserializationSchema();
    }

    public KafakJavaSource topic(String... topic) {
        this.topics = topic;
        return this;
    }

    public KafakJavaSource alias(String alias) {
        this.alias = alias;
        return this;
    }

    public KafakJavaSource deserializer(KafkaDeserializationSchema<T> deserializer) {
        this.deserializer = deserializer;
        return this;
    }

    public KafakJavaSource assigner(AssignerWithPeriodicWatermarks<KafkaRecord<T>> assigner) {
        this.assigner = assigner;
        return this;
    }

    public DataStreamSource<KafkaRecord<T>> getDataStream() {
        FlinkKafkaConsumer011<KafkaRecord<T>> consumer = KafkaSource.getSource(ctx,this.topics,this.alias,this.deserializer,this.assigner,null);
        return ctx.getJavaEnv().addSource(consumer);
    }

}
