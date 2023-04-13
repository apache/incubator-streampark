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

import org.apache.streampark.shaded.ch.qos.logback.classic.LoggerContext
import org.apache.streampark.shaded.ch.qos.logback.classic.joran.JoranConfigurator
import org.apache.streampark.shaded.ch.qos.logback.classic.util.ContextSelectorStaticBinder
import org.apache.streampark.shaded.ch.qos.logback.classic.util.{ContextInitializer => LogBackContextInitializer}
import org.apache.streampark.shaded.ch.qos.logback.core.{CoreConstants, LogbackException}
import org.apache.streampark.shaded.ch.qos.logback.core.status.StatusUtil
import org.apache.streampark.shaded.ch.qos.logback.core.util.StatusPrinter

import org.apache.streampark.shaded.org.slf4j.{ILoggerFactory, Logger => Slf4JLogger}
import org.apache.streampark.shaded.org.slf4j.spi.LoggerFactoryBinder

import java.io.{ByteArrayInputStream, File}
import java.net.URL
import java.nio.charset.StandardCharsets
import scala.util.Try
import scala.util.Success
import scala.util.Failure

trait Logger {

  @transient private[this] var _logger: Slf4JLogger = _

  private[this] val prefix = "[StreamPark]"

  protected def logName: String = this.getClass.getName.stripSuffix("$")

  protected def logger: Slf4JLogger = {
    if (_logger == null) {
      initializeLogging()
      val factory = LoggerFactory.getLoggerFactory()
      _logger = factory.getLogger(logName)
    }
    _logger
  }

  def logInfo(msg: => String) {
    logger.info(s"$prefix $msg")
  }

  def logInfo(msg: => String, throwable: Throwable) {
    logger.info(s"$prefix $msg", throwable)
  }

  def logDebug(msg: => String) {
    logger.debug(s"$prefix $msg")
  }

  def logDebug(msg: => String, throwable: Throwable) {
    logger.debug(s"$prefix $msg", throwable)
  }

  def logTrace(msg: => String) {
    logger.trace(s"$prefix $msg")
  }

  def logTrace(msg: => String, throwable: Throwable) {
    logger.trace(s"$prefix $msg", throwable)
  }

  def logWarn(msg: => String) {
    logger.warn(s"$prefix $msg")
  }

  def logWarn(msg: => String, throwable: Throwable) {
    logger.warn(s"$prefix $msg", throwable)
  }

  def logError(msg: => String) {
    logger.error(s"$prefix $msg")
  }

  def logError(msg: => String, throwable: Throwable) {
    logger.error(s"$prefix $msg", throwable)
  }

  protected def initializeLogging(): Unit = {
    if (!Logger.initialized) {
      Logger.initLock.synchronized {
        if (!Logger.initialized) {
          Logger.initialized = true
          logger
        }
      }
    }
  }

}

private object Logger {
  @volatile private var initialized = false
  val initLock = new Object()
}

private[this] object LoggerFactory extends LoggerFactoryBinder {

  private lazy val contextSelectorBinder: ContextSelectorStaticBinder = {
    val defaultLoggerContext = new LoggerContext
    Try(new ContextInitializer(defaultLoggerContext).autoConfig()) match {
      case Success(s) => s
      case Failure(e) =>
        val msg = "Failed to auto configure default logger context"
        // scalastyle:off println
        System.err.println(msg)
        // scalastyle:off println
        System.err.println("Reported exception:")
        e.printStackTrace()
    }
    if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
      StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext)
    }
    val selectorBinder = new ContextSelectorStaticBinder()
    selectorBinder.init(defaultLoggerContext, new Object())
    selectorBinder
  }

  override def getLoggerFactory: ILoggerFactory = {
    if (contextSelectorBinder.getContextSelector == null) {
      throw new IllegalStateException("contextSelector cannot be null. See also " + CoreConstants.CODES_URL + "#null_CS")
    }
    contextSelectorBinder.getContextSelector.getLoggerContext
  }

  override def getLoggerFactoryClassStr: String = contextSelectorBinder.getClass.getName

  private class ContextInitializer(loggerContext: LoggerContext) extends LogBackContextInitializer(loggerContext) {
    override def configureByResource(url: URL): Unit = {
      Utils.notNull(url, "URL argument cannot be null")
      val path = url.getPath
      if (path.endsWith("xml")) {
        val configurator = new JoranConfigurator()
        configurator.setContext(loggerContext)
        val text = FileUtils.readString(new File(path))
          .replaceAll(
            "ch.qos.logback",
            "org.apache.streampark.shaded.ch.qos.logback")
          .replaceAll(
            "org.slf4j",
            "org.apache.streampark.shaded.org.slf4j")
          .replaceAll(
            "org.apache.log4j",
            "org.apache.streampark.shaded.org.apache.log4j")
          .replaceAll(
            "org.apache.logging.log4j",
            "org.apache.streampark.shaded.org.apache.logging.log4j")

        val input = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))
        configurator.doConfigure(input)
      } else throw {
        new LogbackException("Unexpected filename extension of file [" + url.toString + "]. Should be .xml")
      }
    }

  }

}


