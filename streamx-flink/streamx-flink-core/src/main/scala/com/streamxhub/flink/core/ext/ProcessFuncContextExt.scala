package com.streamxhub.flink.core.ext

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag

/**
 * 扩展 ProcessFunction方法
 *
 * @param ctx
 * @tparam IN
 * @tparam OUT
 */
class ProcessFuncContextExt[IN, OUT](val ctx: ProcessFunction[IN, OUT]#Context) {

  def sideOut[R: TypeInformation](outputTag: String, value: R): Unit = {
    val tag = new OutputTag[R](outputTag)
    ctx.output[R](tag, value)
  }

}
