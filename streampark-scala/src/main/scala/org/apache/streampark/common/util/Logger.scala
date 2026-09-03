/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.common.util

import org.apache.streampark.shaded.org.slf4j.{Logger => Slf4JLogger}

trait Logger {

  @transient private[this] var _logger: Slf4JLogger = _

  private[this] val prefix = "[StreamPark]"

  protected def logName: String = this.getClass.getName.stripSuffix("$")

  protected def logger: Slf4JLogger = {
    if (_logger == null) {
      _logger = StreamParkLoggerFactory.loggerFactory().getLogger(logName)
    }
    _logger
  }

  protected def logInfo(msg: => String): Unit = {
    if (logger.isInfoEnabled) logger.info(s"$prefix $msg")
  }

  protected def logInfo(msg: => String, throwable: Throwable): Unit = {
    if (logger.isInfoEnabled) logger.info(s"$prefix $msg", throwable)
  }

  protected def logDebug(msg: => String): Unit = {
    if (logger.isDebugEnabled) logger.debug(s"$prefix $msg")
  }

  protected def logDebug(msg: => String, throwable: Throwable): Unit = {
    if (logger.isDebugEnabled) logger.debug(s"$prefix $msg", throwable)
  }

  def logTrace(msg: => String): Unit = {
    if (logger.isTraceEnabled) logger.trace(s"$prefix $msg")
  }

  protected def logTrace(msg: => String, throwable: Throwable): Unit = {
    if (logger.isTraceEnabled) logger.trace(s"$prefix $msg", throwable)
  }

  def logWarn(msg: => String): Unit = {
    if (logger.isWarnEnabled) logger.warn(s"$prefix $msg")
  }

  protected def logWarn(msg: => String, throwable: Throwable): Unit = {
    if (logger.isWarnEnabled) logger.warn(s"$prefix $msg", throwable)
  }

  protected def logError(msg: => String): Unit = {
    if (logger.isErrorEnabled) logger.error(s"$prefix $msg")
  }

  protected def logError(msg: => String, throwable: Throwable): Unit = {
    if (logger.isErrorEnabled) logger.error(s"$prefix $msg", throwable)
  }

  protected def isTraceEnabled(): Boolean = logger.isTraceEnabled
}
