package com.streamxhub.spark.monitor.controller

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import org.json4s._

class BaseController {

  @transient
  implicit val format: AnyRef with Formats = Serialization.formats(NoTypeHints)

  @transient
  implicit val seconds: FiniteDuration = 60.second

  implicit def render(future:Future[_]):Render = {
    new Render(future)
  }

  class Render(future: Future[_]) {
    def render(): String = {
      write(Await.result(future, seconds))
    }
  }
}


