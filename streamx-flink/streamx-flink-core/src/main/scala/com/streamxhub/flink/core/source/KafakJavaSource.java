package com.streamxhub.flink.core.source;

import com.streamxhub.flink.core.StreamingContext;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.scala.DataStream;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;

import java.util.Collections;
import static scala.collection.JavaConversions.*;

public class KafakJavaSource<T> extends KafkaSource {

    private String[] topics;
    private String alias = "";
    private KafkaDeserializationSchema<T> deserializer;
    private AssignerWithPeriodicWatermarks<KafkaRecord<T>> assigner;

    public KafakJavaSource(StreamingContext ctx) {
        super(ctx, mapAsScalaMap(Collections.EMPTY_MAP) );
    }

    public KafakJavaSource topic(String...topic) {
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

    public  DataStream<KafkaRecord<T>> getDataStream(TypeInformation typeInformation) {
        return super.getDataStream(topics, alias, deserializer, assigner,typeInformation);
    }
}
