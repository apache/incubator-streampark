package com.streamxhub.flink.core.enums


object RestartStrategy extends Enumeration {
  type RestartStrategy = Value
  val `fixed-delay`, `failure-rate`, `none` = Value
}


