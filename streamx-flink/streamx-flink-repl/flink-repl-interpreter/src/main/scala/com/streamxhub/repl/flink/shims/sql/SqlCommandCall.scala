package com.streamxhub.repl.flink.shims.sql


/**
 * Call of SQL command with operands and command type.
 */
class SqlCommandCall(val command: SqlCommand, val operands: Array[String], val sql: String) {

  def canEqual(other: Any): Boolean = other.isInstanceOf[SqlCommandCall]

  override def equals(other: Any): Boolean = other match {
    case that: SqlCommandCall =>
      (that canEqual this) &&
        command == that.command &&
        operands == that.operands &&
        sql == that.sql
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(command, operands, sql)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}