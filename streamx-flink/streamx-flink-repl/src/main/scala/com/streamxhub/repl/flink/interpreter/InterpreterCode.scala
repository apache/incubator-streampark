package com.streamxhub.repl.flink.interpreter


object InterpreterResult extends Enumeration {
  type Code = Value
  val SUCCESS, INCOMPLETE, ERROR, KEEP_PREVIOUS_RESULT = Value
}


class InterpreterResult(val code: InterpreterResult.Code) extends Serializable {

}
