package com.streamxhub.flink.core.enums

object StateBackend extends Enumeration {
  type StateBackend = Value
  val jobmanager, filesystem, rocksdb = Value
}
