package com.streamxhub.flink.core.sink

import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import com.streamxhub.flink.core.StreamingContext
import org.elasticsearch.action.index.IndexRequest

import scala.collection.Map

object ESSink {

  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            name: String = null,
            parallelism: Int = 0,
            uidHash: String = null): ESSink = new ESSink(ctx, overwriteParams, name,parallelism, uidHash)

}

class ESSink(@transient context: StreamingContext,
             overwriteParams: Map[String, String] = Map.empty[String, String],
             name: String = null,
             parallelism: Int = 0,
             uidHash: String = null) {

  /**
   * for ElasticSearch5....
   *
   * @param stream
   * @param suffix
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def sink5[T](stream: DataStream[T],
               suffix: String = "",
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              (implicit f: T => IndexRequest): DataStreamSink[T] = {

    //TODO....
    null
    //new sink5(context, overwriteParams, parallelism, uidHash).sink[T](stream, suffix, failureHandler)(f)
  }

  /**
   * for ElasticSearch6....
   *
   * @param stream
   * @param suffix
   * @param restClientFactory
   * @param failureHandler
   * @param f
   * @tparam T
   * @return
   */
  def sink6[T](stream: DataStream[T],
               suffix: String = "",
               restClientFactory: Any = null,
               failureHandler: ActionRequestFailureHandler = new RetryRejectedExecutionFailureHandler)
              (implicit f: T => IndexRequest): DataStreamSink[T] = {

    new ES6Sink(context, overwriteParams, name, parallelism, uidHash).sink[T](stream, suffix, failureHandler)(f)
  }

}
