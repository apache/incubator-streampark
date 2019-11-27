package com.streamxhub.flink.core.util

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.XContentType


object EsIndexUtils {

  def indexRequest(index: String, indexType: String, id: String, source: String)(implicit xContentType: XContentType = XContentType.JSON): IndexRequest = {
    require(source != null, "indexRequest error:source can not be null...")
    require(xContentType != null, "indexRequest error:xContentType can not be null...")
    val indexReq = new IndexRequest(index, indexType, id)
    val mapping = List("source" -> new BytesArray(source), "contentType" -> xContentType)
    mapping.foreach { x =>
      val field = indexReq.getClass.getDeclaredField(x._1)
      field.setAccessible(true)
      field.set(indexReq, x._2)
    }
    indexReq
  }


}
