package com.streamxhub.spark.monitor.core.service

import java.io.StringReader
import java.util.Properties

import com.streamxhub.spark.monitor.api.Const
import com.streamxhub.spark.monitor.api.util.PropertiesUtil
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import scala.collection.JavaConverters._

@Slf4j
@Service("watcherService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Array(classOf[Exception]))
class WatcherService {

  def config(id: String, conf: Map[String,String]): Unit = {
    System.out.println(id + ":config")
  }

  def publish(id: String, conf: Map[String,String]): Unit = {
    System.out.println(id + ":start")
  }

  def shutdown(id: String, conf: Map[String,String]): Unit = {
    System.out.println(id + ":shutdown")
  }


}
