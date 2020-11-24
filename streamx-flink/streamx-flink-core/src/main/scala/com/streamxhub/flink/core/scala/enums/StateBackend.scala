package com.streamxhub.flink.core.scala.enums

object StateBackend extends Enumeration {
  type StateBackend = Value
  val jobmanager, filesystem, rocksdb = Value
}
