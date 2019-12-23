package com.streamxhub.flink.core.sink

import org.apache.commons.lang3.StringUtils
import org.apache.flink.streaming.api.datastream.{DataStream, DataStreamSink}


trait Sink extends Serializable {

  //def sink[T](stream: DataStream[T]): DataStreamSink[T]

  def afterSink[T](sink: DataStreamSink[T], name: String, parallelism: Int, uidHash: String): DataStreamSink[T] = {
    if (name != null && name.nonEmpty) {
      sink.name(name)
    }
    if (parallelism > 0) {
      sink.setParallelism(parallelism)
    }
    if (uidHash != null && uidHash.nonEmpty) {
      sink.setUidHash(uidHash)
    }
    sink
  }

}
