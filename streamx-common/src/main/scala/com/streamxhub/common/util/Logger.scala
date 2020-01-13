/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.common.util

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

