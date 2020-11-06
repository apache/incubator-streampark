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
package com.streamxhub.repl.flink.interpreter

import java.util.Properties

import com.streamxhub.common.util.ClassLoaderUtils
import com.streamxhub.repl.flink.shims.FlinkShims
import org.apache.zeppelin.interpreter.InterpreterContext
import org.slf4j.LoggerFactory


/**
 * Interpreter for flink scala. It delegates all the function to FlinkScalaInterpreter.
 */
class FlinkInterpreter(properties: Properties) {

  private lazy val LOGGER = LoggerFactory.getLogger(classOf[FlinkScalaInterpreter])
  private var interpreter: FlinkScalaInterpreter = _

  private def checkScalaVersion(): Unit = {
    val scalaVersionString = scala.util.Properties.versionString
    LOGGER.info("Using Scala: " + scalaVersionString)
  }

  @throws[Exception] def open(): Unit = {
    checkScalaVersion()
    this.interpreter = new FlinkScalaInterpreter(properties)
    this.interpreter.open()
  }

  @throws[Exception] def close(): Unit = if (this.interpreter != null) this.interpreter.close()

  @throws[Exception] def interpret(code: String, out: InterpreterOutput): InterpreterResult = {
    this.open()
    // set ClassLoader of current Thread to be the ClassLoader of Flink scala-shell,
    // otherwise codegen will fail to find classes defined in scala-shell
    ClassLoaderUtils.runAsClassLoader(getFlinkScalaShellLoader, () => {
      interpreter.interpret(code, out)
    })
  }

  @throws[Exception] def cancel(context: InterpreterContext): Unit = {
    this.interpreter.cancel(context)
  }

  private[flink] def getExecutionEnvironment = this.interpreter.getExecutionEnvironment()

  private[flink] def getStreamExecutionEnvironment = this.interpreter.getStreamExecutionEnvironment()

  private[flink] def getJobManager = this.interpreter.getJobManager

  private[flink] def getDefaultParallelism: Int = this.interpreter.defaultParallelism

  def getFlinkScalaShellLoader: ClassLoader = interpreter.getFlinkScalaShellLoader

  private[flink] def getFlinkConfiguration = this.interpreter.getConfiguration

  def getFlinkShims: FlinkShims = this.interpreter.getFlinkShims

  def setSavepointIfNecessary(context: InterpreterContext): Unit = {
    this.interpreter.setSavepointPathIfNecessary(context)
  }

  def setParallelismIfNecessary(context: InterpreterContext): Unit = {
    this.interpreter.setParallelismIfNecessary(context)
  }

}
