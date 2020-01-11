package com.streamxhub.common.util

import scalaj.http.Http
import scala.collection.Map

object HttpUtils {

  def httpPost(url: String, data: String, headers: Map[String, String] = Map.empty[String, String]): (Int, String) = {
    var req = Http(url).postData(data)
    headers.foreach { case (k, v) => req = req.header(k, v) }
    val res = req.asString
    (res.code, res.body)
  }

  def httpGet(url: String, param: Seq[(String, String)] = Seq.empty[(String, String)]): (Int, String) = {
    val req = Http(url).params(param)
    val res = req.asString
    (res.code, res.body)
  }
}
