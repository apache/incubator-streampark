package com.streamxhub.flink.core.sink.java;

import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.sink.scala.KafkaEqualityPartitioner;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import java.util.Map;
import static scala.collection.JavaConversions.*;

public class KafkaSink<T> {

    private StreamingContext context;
    //common param...
    private Map<String,String> param;
    private Integer parallelism;
    private String name;
    private String uid;
    //---end---

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

    public KafkaSink<T> param(Map<String,String> param) {
        this.param = param;
        return this;
    }

    /**
     * 设置要下沉的topic
     *
     * @param topic
     * @return
     */
    public KafkaSink<T> topic(String topic) {
        this.topic = topic;
        return this;
    }

    /**
     * set SerializationSchema
     *
     * @param serializer
     * @return
     */
    public KafkaSink<T> serializer(SerializationSchema<T> serializer) {
        this.serializer = serializer;
        return this;
    }

    /**
     * set FlinkKafkaPartitioner
     *
     * @param partitioner
     * @return
     */
    public KafkaSink<T> partitioner(FlinkKafkaPartitioner<T> partitioner) {
        this.partitioner = partitioner;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> source) {
        return this.sink(source, this.topic);
    }

    public DataStreamSink<T> sink(DataStream<T> source, String topic) {
        this.topic(topic);
        com.streamxhub.flink.core.sink.scala.KafkaSink sink = new com.streamxhub.flink.core.sink.scala.KafkaSink(this.context,mapAsScalaMap(this.param),this.parallelism,this.name,this.uid);
       return sink.javaSink(source,this.topic,this.serializer,this.partitioner);
    }
}
