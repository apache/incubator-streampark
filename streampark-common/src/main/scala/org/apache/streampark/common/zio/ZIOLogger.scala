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

package org.apache.streampark.common.zio

import org.apache.streampark.common.util.Logger

import zio.{Cause, FiberId, FiberRefs, LogLevel, LogSpan, Runtime, Trace, ZLayer, ZLogger}
import zio.logging.LoggerNameExtractor

import scala.collection.concurrent.TrieMap

/** ZIOLogger that bridging to [[org.apache.streampark.common.util.Logger]] */
object ZIOLogger {

  lazy val default: ZLayer[Any, Nothing, Unit] = Runtime.addLogger(provideLogger())

  private val defaultLoggerName = getClass.getName
  private val loggers = TrieMap[String, BridgeLogger]()

  private def getLogger(loggerName: String): BridgeLogger = {
    loggers.getOrElseUpdate(loggerName, BridgeLogger(loggerName))
  }

  private case class BridgeLogger(loggerName: String) extends Logger {
    override protected def logName: String = loggerName

    def trace(msg: String): Unit = super.logTrace(msg)
    def info(msg: String): Unit = super.logInfo(msg)
    def warn(msg: String): Unit = super.logWarn(msg)
    def error(msg: String): Unit = super.logError(msg)
    def debug(msg: String): Unit = super.logDebug(msg)
  }

  private[this] def provideLogger(): ZLogger[String, Unit] = (
      trace: Trace,
      _: FiberId,
      logLevel: LogLevel,
      message: () => String,
      _: Cause[Any],
      _: FiberRefs,
      _: List[LogSpan],
      annotations: Map[String, String]) => {

    val loggerName =
      LoggerNameExtractor.trace(trace, FiberRefs.empty, Map.empty).getOrElse(defaultLoggerName)
    val logger = getLogger(loggerName)
    val msg =
      if (annotations.nonEmpty)
        s"${annotations.map { case (k, v) => s"[$k=$v]" }.mkString(" ")} ${message()}"
      else message()

    logLevel match {
      case LogLevel.None => logger.trace(msg)
      case LogLevel.All => logger.trace(msg)
      case LogLevel.Trace => logger.trace(msg)
      case LogLevel.Debug => logger.debug(msg)
      case LogLevel.Info => logger.info(msg)
      case LogLevel.Warning => logger.warn(msg)
      case LogLevel.Error => logger.error(msg)
      case LogLevel.Fatal => logger.error(msg)
      case _ =>
    }
  }

}
