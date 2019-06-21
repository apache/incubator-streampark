package com.streamxhub.spark.monitor.core.service

import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Service("watcherService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Array(classOf[Exception]))
class WatcherService {

  def config(id: String, conf: Map[String, String]): Unit = {
    System.out.println(id + ":config")
  }

  def publish(id: String): Unit = {
    System.out.println(id + ":start")
  }

  def shutdown(id: String): Unit = {
    System.out.println(id + ":shutdown")
  }


}
