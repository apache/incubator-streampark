package com.streamxhub.repl.flink.interpreter

import java.io.{Serializable}
import java.util

import com.google.gson.Gson
import scala.collection.JavaConversions._

object InterpreterResult {
  private val gson: Gson = new Gson

  object Code extends Enumeration {
    type Code = Value
    val SUCCESS, INCOMPLETE, ERROR, KEEP_PREVIOUS_RESULT = Value
  }

  def fromJson(json: String): InterpreterResult = gson.fromJson(json, classOf[InterpreterResult])
}

class InterpreterResult extends Serializable {
  private[interpreter] var code: InterpreterResult.Code.Value = null
  private[interpreter] val message = new util.LinkedList[String]

  def this(code: InterpreterResult.Code.Value) {
    this()
    this.code = code
  }

  def this(code: InterpreterResult.Code.Value, msgs: util.List[String]) {
    this()
    this.code = code
    message.addAll(msgs)
  }

  def this(code: InterpreterResult.Code.Value, msg: String) {
    this()
    this.code = code
    add(msg)
  }

  def add(interpreterResultMessage: String): Unit = {
    message.add(interpreterResultMessage)
  }

  def toJson: String = InterpreterResult.gson.toJson(this)

  override def toString: String = {
    val sb = new StringBuilder
    for (m <- message) {
      sb.append(m).append("\n")
    }
    sb.toString
  }
}
