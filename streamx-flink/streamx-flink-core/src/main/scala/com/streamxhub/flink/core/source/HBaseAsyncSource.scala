package com.streamxhub.flink.core.source

import com.streamxhub.flink.core.StreamingContext

import scala.annotation.meta.param
import scala.collection.Map


object HBaseAsyncSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): HBaseAsyncSource = new HBaseAsyncSource(ctx, overrideParams)

}
/*
 * @param ctx
 * @param overrideParams
 */
class HBaseAsyncSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

}
