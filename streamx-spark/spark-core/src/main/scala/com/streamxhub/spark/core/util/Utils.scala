package com.streamxhub.spark.core.util

import java.io.{File, FileInputStream, IOException, InputStreamReader}
import java.util.Properties

import org.apache.spark.SparkException

import scala.collection.JavaConverters._
import scala.collection.Map

import scalaj.http._

/**
  *
  */
object Utils {


  /** Load properties present in the given file. */
  def getPropertiesFromFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile, s"Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), "UTF-8")
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().asScala.map(
        k => (k, properties.getProperty(k).trim)).toMap
    } catch {
      case e: IOException =>
        throw new SparkException(s"Failed when loading Spark properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  def classForName(className: String): Class[_] = {
    Class.forName(className, true, Thread.currentThread().getContextClassLoader)
  }

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


  def main(args: Array[String]): Unit = {

    println(Utils.httpGet("http://bigdata-dev:9200/app/heartbeat/application_1526125629402_7874/15000", Seq.empty[(String, String)]))
  }
}
