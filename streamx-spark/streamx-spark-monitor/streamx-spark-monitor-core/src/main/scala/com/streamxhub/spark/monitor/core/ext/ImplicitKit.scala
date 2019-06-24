package com.streamxhub.spark.monitor.core.ext

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ImplicitKit {

  @transient
  private[this] implicit val format: AnyRef with Formats = Serialization.formats(NoTypeHints)

  @transient
  private[this] implicit val seconds: FiniteDuration = 60.second

  implicit def writeFuture(future:Future[_]):String = {
    write(Await.result(future, seconds))
  }

}


