package com.streamxhub.spark.core.channel

import org.apache.spark.streaming.dstream.DStream

/**
  *
  */
trait Channel[M, N] {

  /**
    * 处理
    *
    * @param dStream
    * @return
    */
  def procese(dStream: DStream[M]): DStream[N]
}
