package com.streamxhub.flink.test

import java.sql.Connection
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.{Condition, ReentrantLock}


import scala.collection.mutable
import scala.util.{Random, Try}
import scala.collection.JavaConversions._

object ThreadTestApp {

  def main(args: Array[String]): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        running("1")
      }
    }).start()

    new Thread(new Runnable {
      override def run(): Unit = {
        running("2")
      }
    }).start()

    new Thread(new Runnable {
      override def run(): Unit = {
        running("3")
      }
    }).start()

  }

  def running(prefix: String) = {
    for (_ <- 1 to 10) {
      new Thread(new Runnable {
        override def run(): Unit = {
          Resource.get(prefix)
        }
      }).start()
    }
  }

}

object Resource {

  private val lock = new ReentrantLock()

  private val connectionCondition: mutable.Map[String, Condition] = new ConcurrentHashMap[String, Condition]()

  private val lockMonitor: mutable.Map[String, String] = new ConcurrentHashMap[String, String]()
  /**
   * 默认一个实例连接池大小...
   */
  private[this] val connectionPool = new ThreadLocal[ConcurrentHashMap[String, util.Queue[String]]]()

  private[this] val dataSourceHolder = new ConcurrentHashMap[String, Random]

  def get(instance: String): String = {
    try {
      lock.lock()
      val cond = connectionCondition.getOrElseUpdate(instance, lock.newCondition())
      while (lockMonitor.contains(instance)) {
        cond.await()
      }
      lockMonitor += instance -> instance
      println(s"${instance}...income")
      if (instance == "1") {
        println("------------")
        Thread.sleep(4000)
      }
      var conn = Try(connectionPool.get()(instance).poll()).getOrElse(null)
      if (conn == null) {
        val ds = Try(dataSourceHolder(instance)).getOrElse(null)
        //没有数据源
        if (ds == null) {
          val random = new Random(instance.toInt)
          conn = random.nextString(instance.toInt)
          dataSourceHolder += instance -> random
        }
      }
      lockMonitor -= instance
      connectionCondition.filter(_._1 == instance).foreach(_._2.signalAll())
      conn
    } finally {
      println(s"${instance}...unlock")
      lock.unlock()
    }
  }

}
