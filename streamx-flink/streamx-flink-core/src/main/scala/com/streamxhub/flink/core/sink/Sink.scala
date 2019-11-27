package com.streamxhub.flink.core.sink

import org.apache.flink.streaming.api.datastream.{DataStream, DataStreamSink}


trait Sink extends Serializable {

  //def sink[T: ClassTag](stream: DataStream[T]): DataStreamSink[T]

  def afterSink[T](sink: DataStreamSink[T], parallelism: Int, uidHash: String): DataStreamSink[T] = {
    val _uidHash = uidHash != null && uidHash.nonEmpty
    val _parallelism = parallelism > 0
    (_parallelism, _uidHash) match {
      case (true, true) => sink.setParallelism(parallelism).setUidHash(uidHash)
      case (true, _) => sink.setParallelism(parallelism)
      case (_, true) => sink.setUidHash(uidHash)
      case _ => sink
    }
  }

}
