package com.streamxhub.common.util


import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpGet, HttpPatch, HttpPost, HttpRequestBase}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.util

import scala.collection.JavaConversions._

object HttpClientUtils {

  private var connectionManager: PoolingHttpClientConnectionManager = _
  private val UTF_8 = "UTF-8"

  private def init(): Unit = {
    if (connectionManager == null) {
      connectionManager = new PoolingHttpClientConnectionManager
      connectionManager.setMaxTotal(50)
      connectionManager.setDefaultMaxPerRoute(5)
    }
  }

  /**
   * 通过连接池获取HttpClient
   *
   * @return
   */
  private def getHttpClient = {
    init()
    HttpClients.custom.setConnectionManager(connectionManager).build
  }

  /**
   * @param url
   * @return
   */
  def httpGetRequest(url: String): String = {
    val httpGet = new HttpGet(url)
    getResult(httpGet)
  }

  @throws[URISyntaxException] def httpGetRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val ub = uriBuilder(url, params)
    val httpGet = new HttpGet(ub.build)
    getResult(httpGet)
  }

  @throws[URISyntaxException] def httpGetRequest(url: String, headers: util.Map[String, AnyRef], params: util.Map[String, AnyRef]): String = {
    val ub = uriBuilder(url, params)
    val httpGet = new HttpGet(ub.build)
    for (param <- headers.entrySet) {
      httpGet.addHeader(param.getKey, String.valueOf(param.getValue))
    }
    getResult(httpGet)
  }

  def uriBuilder(url: String, params: util.Map[String, AnyRef]): URIBuilder = {
    val uriBuilder = new URIBuilder
    uriBuilder.setPath(url)
    uriBuilder.setParameters(params2NVPS(params))
    uriBuilder
  }

  def httpPostRequest(url: String): String = {
    val httpPost = new HttpPost(url)
    getResult(httpPost)
  }

  def httpPatchRequest(url: String): String = {
    val httpPatch = new HttpPatch(url)
    getResult(httpPatch)
  }

  @throws[UnsupportedEncodingException] def httpPostRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val httpPost = new HttpPost(url)
    httpPost.setEntity(new UrlEncodedFormEntity(params2NVPS(params), UTF_8))
    getResult(httpPost)
  }

  @throws[UnsupportedEncodingException] def httpPatchRequest(url: String, params: util.Map[String, AnyRef]): String = {
    val httpPatch = new HttpPatch(url)
    httpPatch.setEntity(new UrlEncodedFormEntity(params2NVPS(params), UTF_8))
    getResult(httpPatch)
  }

  @throws[UnsupportedEncodingException] def httpPostRequest(url: String, params: String): String = {
    val httpPost = new HttpPost(url)
    val entity = new StringEntity(params, "utf-8") //解决中文乱码问题
    entity.setContentEncoding("UTF-8")
    entity.setContentType("application/json")
    httpPost.setEntity(entity)
    getResult(httpPost)
  }

  @throws[UnsupportedEncodingException] def httpPatchRequest(url: String, params: String): String = {
    val httpPost = new HttpPatch(url)
    val entity = new StringEntity(params, "utf-8") //解决中文乱码问题
    entity.setContentEncoding("UTF-8")
    entity.setContentType("application/json")
    httpPost.setEntity(entity)
    getResult(httpPost)
  }

  @throws[UnsupportedEncodingException] def httpPostRequest(url: String, params: util.Map[String, AnyRef], headers: util.Map[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    val httpPost = new HttpPost(url)
    for (param <- headers.entrySet) {
      httpPost.addHeader(param.getKey, String.valueOf(param.getValue))
    }
    httpPost.setEntity(new UrlEncodedFormEntity(params2NVPS(params), UTF_8))
    getResult(httpPost)
  }

  @throws[UnsupportedEncodingException] def httpPatchRequest(url: String, params: util.Map[String, AnyRef], headers: util.Map[String, AnyRef] = Map.empty[String, AnyRef]): String = {
    val httpPatch = new HttpPatch(url)
    for (param <- headers.entrySet) {
      httpPatch.addHeader(param.getKey, String.valueOf(param.getValue))
    }
    httpPatch.setEntity(new UrlEncodedFormEntity(params2NVPS(params), UTF_8))
    getResult(httpPatch)
  }

  private[this] def params2NVPS(params: util.Map[String, AnyRef]) = {
    val pairs = new util.ArrayList[NameValuePair]
    for (param <- params.entrySet) {
      pairs.add(new BasicNameValuePair(param.getKey, String.valueOf(param.getValue)))
    }
    pairs
  }

  /**
   * 处理Http请求
   *
   * @param request
   * @return
   */
  private def getResult(request: HttpRequestBase): String = {
    val httpClient = getHttpClient
    try {
      val response = httpClient.execute(request)
      val entity = response.getEntity
      if (entity != null) { // long len = entity.getContentLength();// -1 表示长度未知
        val result = EntityUtils.toString(entity)
        response.close()
        result
      } else null
    } catch {
      case e: Exception => throw e
    }
  }

}
