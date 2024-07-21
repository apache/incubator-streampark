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

package org.apache.streampark.flink.core.conf

import org.apache.streampark.common.util.Implicits._

import org.apache.commons.cli.{CommandLine, DefaultParser, Option, Options}

import java.lang.{Boolean => JavaBoolean}

import scala.util.{Failure, Success, Try}

/** Applies to all optional parameters under flink run */
object FlinkRunOption {

  val HELP_OPTION = new Option("h", "help", false, null)
  val JAR_OPTION = new Option("j", "jarfile", true, null)
  val CLASS_OPTION = new Option("c", "class", true, null)
  val CLASSPATH_OPTION = new Option("C", "classpath", true, null)
  val PARALLELISM_OPTION = new Option("p", "parallelism", true, null)
  val DETACHED_OPTION = new Option("d", "detached", false, null)
  val SHUTDOWN_IF_ATTACHED_OPTION = new Option("sae", "shutdownOnAttachedExit", false, null)
  val YARN_DETACHED_OPTION = new Option("yd", "yarndetached", false, null)
  val ARGS_OPTION = new Option("a", "arguments", true, null)
  val ADDRESS_OPTION = new Option("m", "jobmanager", true, null)
  val SAVEPOINT_PATH_OPTION = new Option("s", "fromSavepoint", true, null)
  val SAVEPOINT_ALLOW_NON_RESTORED_OPTION = new Option("n", "allowNonRestoredState", false, null)
  val SAVEPOINT_DISPOSE_OPTION = new Option("d", "dispose", true, null)
  // list specific options
  val RUNNING_OPTION = new Option("r", "running", false, null)
  val SCHEDULED_OPTION = new Option("s", "scheduled", false, null)
  val ALL_OPTION = new Option("a", "all", false, null)
  val ZOOKEEPER_NAMESPACE_OPTION = new Option("z", "zookeeperNamespace", true, null)
  val CANCEL_WITH_SAVEPOINT_OPTION = new Option("s", "withSavepoint", true, null)
  val STOP_WITH_SAVEPOINT_PATH = new Option("p", "savepointPath", true, null)
  val STOP_AND_DRAIN = new Option("d", "drain", false, null)
  val PY_OPTION = new Option("py", "python", true, null)
  val PYFILES_OPTION = new Option("pyfs", "pyFiles", true, null)
  val PYMODULE_OPTION = new Option("pym", "pyModule", true, null)
  val PYREQUIREMENTS_OPTION = new Option("pyreq", "pyRequirements", true, null)
  val PYARCHIVE_OPTION = new Option("pyarch", "pyArchives", true, null)
  val PYEXEC_OPTION = new Option("pyexec", "pyExecutable", true, null)
  val EXECUTOR_OPTION = new Option("e", "executor", true, null)
  val TARGET_OPTION = new Option("t", "target", true, null)

  val DYNAMIC_PROPERTIES = Option.builder("D")
    .argName("property=value")
    .numberOfArgs(2)
    .valueSeparator('=')
    .build

  HELP_OPTION.setRequired(false)

  JAR_OPTION.setRequired(false)
  JAR_OPTION.setArgName("jarfile")

  CLASS_OPTION.setRequired(false)
  CLASS_OPTION.setArgName("classname")

  CLASSPATH_OPTION.setRequired(false)
  CLASSPATH_OPTION.setArgName("url")

  PARALLELISM_OPTION.setRequired(false)
  PARALLELISM_OPTION.setArgName("parallelism")

  DETACHED_OPTION.setRequired(false)
  SHUTDOWN_IF_ATTACHED_OPTION.setRequired(false)
  YARN_DETACHED_OPTION.setRequired(false)

  ARGS_OPTION.setRequired(false)
  ARGS_OPTION.setArgName("programArgs")
  ARGS_OPTION.setArgs(Option.UNLIMITED_VALUES)

  RUNNING_OPTION.setRequired(false)
  SCHEDULED_OPTION.setRequired(false)

  SAVEPOINT_PATH_OPTION.setRequired(false)
  SAVEPOINT_PATH_OPTION.setArgName("savepointPath")

  SAVEPOINT_ALLOW_NON_RESTORED_OPTION.setRequired(false)

  ZOOKEEPER_NAMESPACE_OPTION.setRequired(false)
  ZOOKEEPER_NAMESPACE_OPTION.setArgName("zookeeperNamespace")

  CANCEL_WITH_SAVEPOINT_OPTION.setRequired(false)
  CANCEL_WITH_SAVEPOINT_OPTION.setArgName("targetDirectory")
  CANCEL_WITH_SAVEPOINT_OPTION.setOptionalArg(true)

  STOP_WITH_SAVEPOINT_PATH.setRequired(false)
  STOP_WITH_SAVEPOINT_PATH.setArgName("savepointPath")
  STOP_WITH_SAVEPOINT_PATH.setOptionalArg(true)

  STOP_AND_DRAIN.setRequired(false)

  PY_OPTION.setRequired(false)
  PY_OPTION.setArgName("pythonFile")

  PYFILES_OPTION.setRequired(false)
  PYFILES_OPTION.setArgName("pythonFiles")

  PYMODULE_OPTION.setRequired(false)
  PYMODULE_OPTION.setArgName("pythonModule")

  PYREQUIREMENTS_OPTION.setRequired(false)

  PYARCHIVE_OPTION.setRequired(false)

  PYEXEC_OPTION.setRequired(false)

  /** @return */
  def allOptions: Options = {
    val commOptions = getRunCommandOptions
    val yarnOptions = getYARNOptions
    val resultOptions = new Options
    commOptions.getOptions.foreach(resultOptions.addOption)
    yarnOptions.getOptions
      .filter(x => !resultOptions.hasOption(x.getOpt))
      .foreach(resultOptions.addOption)
    resultOptions
  }

  def getRunCommandOptions: Options = {
    var options = buildGeneralOptions(new Options)
    options = getProgramSpecificOptions(options)
    options.addOption(SAVEPOINT_PATH_OPTION)
    options.addOption(EXECUTOR_OPTION)
    options.addOption(TARGET_OPTION)
    options.addOption(SAVEPOINT_ALLOW_NON_RESTORED_OPTION)
    options.addOption(DYNAMIC_PROPERTIES)
  }

  def getYARNOptions: Options = {
    val allOptions = new Options
    allOptions.addOption(DETACHED_OPTION)
    allOptions.addOption(YARN_DETACHED_OPTION)
    allOptions
  }

  def buildGeneralOptions(options: Options): Options = {
    options.addOption(HELP_OPTION)
    // backwards compatibility: ignore verbose flag (-v)
    options.addOption(new Option("v", "verbose", false, "This option is deprecated."))
    options
  }

  def getProgramSpecificOptions(options: Options): Options = {
    options.addOption(JAR_OPTION)
    options.addOption(CLASS_OPTION)
    options.addOption(ADDRESS_OPTION)
    options.addOption(CLASSPATH_OPTION)
    options.addOption(PARALLELISM_OPTION)
    options.addOption(ARGS_OPTION)
    options.addOption(DETACHED_OPTION)
    options.addOption(SHUTDOWN_IF_ATTACHED_OPTION)
    options.addOption(YARN_DETACHED_OPTION)
    options.addOption(PY_OPTION)
    options.addOption(PYFILES_OPTION)
    options.addOption(PYMODULE_OPTION)
    options.addOption(PYREQUIREMENTS_OPTION)
    options.addOption(PYARCHIVE_OPTION)
    options.addOption(PYEXEC_OPTION)
    options
  }

  def mergeOptions(optionsA: Options, optionsB: Options): Options = {
    val resultOptions = new Options
    require(optionsA != null)
    require(optionsB != null)
    optionsA.getOptions.foreach(resultOptions.addOption)
    optionsB.getOptions.foreach(resultOptions.addOption)
    resultOptions
  }

  def parse(options: Options, args: Array[String], stopAtNonOptions: JavaBoolean): CommandLine = {
    val parser = new DefaultParser
    Try(parser.parse(options, args, stopAtNonOptions)) match {
      case Success(value) => value
      case Failure(e) => throw e
    }
  }

}
