package com.streamxhub.flink.core.sink;

import com.streamxhub.flink.core.StreamingContext;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import java.util.Map;

public class KafkaJavaSink<T>  {

    private StreamingContext context;
    private String topic;
    private String alias;
    private SerializationSchema<T> serializer;
    private FlinkKafkaPartitioner<T> partitioner;
    private Map<String,String> param;

    public KafkaJavaSink(StreamingContext context) {
        this.context = context;
        //默认partitioner为KafkaEqualityPartitioner
        partitioner = new KafkaEqualityPartitioner<T>(context.getParallelism());
    }

    public KafkaJavaSink<T> alias(String v) {
        this.alias = alias;
        return this;
    }

    /**
     * 设置要下沉的topic
     *
     * @param topic
     * @return
     */
    public KafkaJavaSink<T> topic(String topic) {
        this.topic = topic;
        return this;
    }

    /**
     * set SerializationSchema
     *
     * @param serializer
     * @return
     */
    public KafkaJavaSink<T> serializer(SerializationSchema<T> serializer) {
        this.serializer = serializer;
        return this;
    }

    /**
     * set FlinkKafkaPartitioner
     *
     * @param partitioner
     * @return
     */
    public KafkaJavaSink<T> partitioner(FlinkKafkaPartitioner<T> partitioner) {
        this.partitioner = partitioner;
        return this;
    }

    public KafkaJavaSink<T> param(Map<String,String> param) {
        this.param = param;
        return this;
    }

    public void sink(DataStream<T> source) {
        this.sink(source, this.topic);
    }

    public void sink(DataStream<T> source, String topic) {
        this.topic(topic);
    }
}
