package com.streamxhub.spark.monitor.core.service

import com.streamxhub.spark.monitor.core.dao.SparkConfDao
import org.springframework.stereotype.Service
import com.streamxhub.spark.monitor.core.domain.SparkConf
import org.springframework.beans.factory.annotation.Autowired

import scala.language.postfixOps

@Service class SparkConfService @Autowired()(sparkConfDao: SparkConfDao) {

  def config(conf: SparkConf): Int = {
    sparkConfDao.get(conf.confId) match {
      case null =>
        sparkConfDao.save(conf) match {
          case 0 => 0
          case 1 => sparkConfDao.saveRecord(conf)
        }
      case old =>
        conf.confVersion.compare(old.confVersion) match {
          case 1 =>
            sparkConfDao.update(conf) match {
              case 0 => 0
              case 1 => sparkConfDao.saveRecord(conf)
            }
          case _ => 0
        }
    }
  }

}
