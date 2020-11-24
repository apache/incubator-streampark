package com.streamxhub.flink.core.scala.sink

import org.apache.flink.streaming.api.datastream.DataStreamSink

trait Sink extends Serializable {

  def afterSink[T](sink: DataStreamSink[T], parallelism: Int, name: String, uid: String): DataStreamSink[T] = {
    if (parallelism > 0) {
      sink.setParallelism(parallelism)
    }
    if (name != null && name.nonEmpty) {
      sink.name(name)
    }
    if (uid != null && uid.nonEmpty) {
      sink.uid(uid)
    }
    sink
  }

}
