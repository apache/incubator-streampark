package com.streamxhub.common.util

/**
 * 这是个特质是hbase-spark中实现用来替代spark-2.0中的内部Logging的
 * 这里直接引用代码,使之成为一个通用的类spark.Logging 的特质
 * 使用方式如同spark.Logging
 */

import org.slf4j.impl.StaticLoggerBinder
import org.slf4j.{LoggerFactory, Logger => SlfLogger}

trait Logger {

  @transient private var _logger: SlfLogger = _

  protected def logName = {
    this.getClass.getName.stripSuffix("$")
  }

  protected def logger: SlfLogger = {
    if (_logger == null) {
      initializeLogIfNecessary(false)
      _logger = LoggerFactory.getLogger(logName)
    }
    _logger
  }

  def logInfo(msg: => String) {
    if (logger.isInfoEnabled) logger.info(msg)
  }

  def logDebug(msg: => String) {
    if (logger.isDebugEnabled) logger.debug(msg)
  }

  def logTrace(msg: => String) {
    if (logger.isTraceEnabled) logger.trace(msg)
  }

  def logWarning(msg: => String) {
    if (logger.isWarnEnabled) logger.warn(msg)
  }

  def logError(msg: => String) {
    if (logger.isErrorEnabled) logger.error(msg)
  }

  def logInfo(msg: => String, throwable: Throwable) {
    if (logger.isInfoEnabled) logger.info(msg, throwable)
  }

  def logDebug(msg: => String, throwable: Throwable) {
    if (logger.isDebugEnabled) logger.debug(msg, throwable)
  }

  def logTrace(msg: => String, throwable: Throwable) {
    if (logger.isTraceEnabled) logger.trace(msg, throwable)
  }

  def logWarning(msg: => String, throwable: Throwable) {
    if (logger.isWarnEnabled) logger.warn(msg, throwable)
  }

  def logError(msg: => String, throwable: Throwable) {
    if (logger.isErrorEnabled) logger.error(msg, throwable)
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
    logger
  }
}

private object Logger {
  @volatile private var initialized = false
  val initLock = new Object()
}

