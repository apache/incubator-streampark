package com.streamxhub.spark.core.util

import java.io.StringWriter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/**
  *
  */
object Json {
  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

  def parse[T](str: String)(implicit manifest: Manifest[T]) = {
    mapper.readValue[T](str)
  }

  def generate(obj: AnyRef): String = {
    val out = new StringWriter
    mapper.writeValue(out, obj)
    out.toString
  }
}
