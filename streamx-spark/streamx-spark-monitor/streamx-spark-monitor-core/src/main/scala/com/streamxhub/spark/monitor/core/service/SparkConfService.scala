package com.streamxhub.spark.monitor.core.service

import com.streamxhub.spark.monitor.core.dao.SparkConfDao
import org.springframework.stereotype.Service
import com.streamxhub.spark.monitor.core.domain.SparkConf
import org.springframework.beans.factory.annotation.Autowired

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

@Service
class SparkConfService @Autowired()(sparkConfDao: SparkConfDao) {

  def config(conf: SparkConf): Future[Int] = {
    val future = sparkConfDao.get(conf.confId)
    future.flatMap {
      case null =>
        sparkConfDao.save(conf).flatMap {
          case 0 => Future(0)
          case 1 => sparkConfDao.saveRecord(conf)
        }
      case map =>
        conf.confVersion.compare(map("CONF_VERSION").toString) match {
          case 1 =>
            sparkConfDao.update(conf).flatMap {
              case 0 => Future(0)
              case 1 => sparkConfDao.saveRecord(conf)
            }
          case _ => Future(0)
        }
    }
  }

}
