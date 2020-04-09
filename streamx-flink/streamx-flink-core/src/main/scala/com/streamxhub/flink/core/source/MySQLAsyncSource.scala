package com.streamxhub.flink.core.source

import com.streamxhub.flink.core.StreamingContext

import scala.annotation.meta.param
import scala.collection.Map


object MySQLAsyncSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): MySQLAsyncSource = new MySQLAsyncSource(ctx, overrideParams)

}

/*
 * @param ctx
 * @param overrideParams
 */
class MySQLAsyncSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

}
