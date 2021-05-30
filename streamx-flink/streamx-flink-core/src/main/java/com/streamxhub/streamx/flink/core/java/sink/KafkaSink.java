package com.streamxhub.streamx.flink.core.java.sink;

import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.sink.KafkaEqualityPartitioner;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;

import java.util.Properties;

public class KafkaSink<T> {

    private final StreamingContext context;
    /**
     * common param
     */
    private Properties property;
    private Integer parallelism;
    private String name;
    private String uid;
    //---end---

    private String alias;
    private String topic;
    private SerializationSchema<T> serializer;
    private FlinkKafkaPartitioner<T> partitioner;

    public KafkaSink(StreamingContext context) {
        this.context = context;
        //默认partitioner为KafkaEqualityPartitioner
        partitioner = new KafkaEqualityPartitioner<T>(context.getParallelism());
    }

    public KafkaSink<T> parallelism(Integer parallelism) {
        this.parallelism = parallelism;
        return this;
    }

    public KafkaSink<T> name(String name) {
        this.name = name;
        return this;
    }

    public KafkaSink<T> uid(String uid) {
        this.uid = uid;
        return this;
    }

    public KafkaSink<T> property(Properties property) {
        this.property = property;
        return this;
    }

    public KafkaSink<T> alias(String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * 设置要下沉的topic
     *
     * @param topic: topic name
     * @return KafkaSink: KafkaSink instance
     */
    public KafkaSink<T> topic(String topic) {
        this.topic = topic;
        return this;
    }

    /**
     * set SerializationSchema
     *
     * @param serializer: serializer
     * @return KafkaSink: KafkaSink instance
     *
     */
    public KafkaSink<T> serializer(SerializationSchema<T> serializer) {
        this.serializer = serializer;
        return this;
    }

    /**
     * set FlinkKafkaPartitioner
     *
     * @param partitioner: FlinkKafkaPartitioner
     * @return KafkaSink: KafkaSink
     *
     * */
    public KafkaSink<T> partitioner(FlinkKafkaPartitioner<T> partitioner) {
        this.partitioner = partitioner;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> source) {
        return this.sink(source, this.topic);
    }

    public DataStreamSink<T> sink(DataStream<T> source, String topic) {
        this.topic(topic);
        com.streamxhub.streamx.flink.core.scala.sink.KafkaSink scalaSink = new com.streamxhub.streamx.flink.core.scala.sink.KafkaSink(this.context, this.property, this.parallelism, this.name, this.uid);
        org.apache.flink.streaming.api.scala.DataStream<T> scalaDataStream = new org.apache.flink.streaming.api.scala.DataStream<>(source);
        return scalaSink.sink(scalaDataStream,this.alias, this.topic, this.serializer, this.partitioner);
    }
}
