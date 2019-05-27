package com.streamxhub.spark.core.util

import org.slf4j.LoggerFactory
import org.slf4j.impl.StaticLoggerBinder
import org.slf4j.{Logger => Slf4jLogger}


/**
  *
  * 这是个特质是hbase-spark中实现用来替代spark-2.0中的内部Logging的
  * 这里直接引用代码,使之成为一个通用的类spark.Logging 的特质
  * 使用方式如同spark.Logging
  */
trait Logger {

  @transient private var log_ : Slf4jLogger = _

  protected def logName = {
    this.getClass.getName.stripSuffix("$")
  }

  protected def log: Slf4jLogger = {
    if (log_ == null) {
      initializeLogIfNecessary(false)
      log_ = LoggerFactory.getLogger(logName)
    }
    log_
  }

  protected def logInfo(msg: => String) {
    if (log.isInfoEnabled) log.info(msg)
  }

  protected def logDebug(msg: => String) {
    if (log.isDebugEnabled) log.debug(msg)
  }

  protected def logTrace(msg: => String) {
    if (log.isTraceEnabled) log.trace(msg)
  }

  protected def logWarning(msg: => String) {
    if (log.isWarnEnabled) log.warn(msg)
  }

  protected def logError(msg: => String) {
    if (log.isErrorEnabled) log.error(msg)
  }

  protected def logInfo(msg: => String, throwable: Throwable) {
    if (log.isInfoEnabled) log.info(msg, throwable)
  }

  protected def logDebug(msg: => String, throwable: Throwable) {
    if (log.isDebugEnabled) log.debug(msg, throwable)
  }

  protected def logTrace(msg: => String, throwable: Throwable) {
    if (log.isTraceEnabled) log.trace(msg, throwable)
  }

  protected def logWarning(msg: => String, throwable: Throwable) {
    if (log.isWarnEnabled) log.warn(msg, throwable)
  }

  protected def logError(msg: => String, throwable: Throwable) {
    if (log.isErrorEnabled) log.error(msg, throwable)
  }

  protected def initializeLogIfNecessary(isInterpreter: Boolean): Unit = {
    if (!Logger.initialized) {
      Logger.initLock.synchronized {
        if (!Logger.initialized) {
          initializeLogging(isInterpreter)
        }
      }
    }
  }

  private def initializeLogging(isInterpreter: Boolean): Unit = {
    StaticLoggerBinder.getSingleton.getLoggerFactoryClassStr
    Logger.initialized = true

    log
  }
}

private object Logger {
  @volatile private var initialized = false
  val initLock = new Object()
}
