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

import org.apache.commons.cli.{CommandLine, DefaultParser, Option, Options}

import java.lang.{Boolean => JavaBoolean}
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

/**
 * Applies to all optional parameters under flink run
 */
object FlinkRunOption {

  val HELP_OPTION = new Option("h", "help", false, "Show the help message for the CLI Frontend or the action.")

  val JAR_OPTION = new Option("j", "jarfile", true, "Flink program JAR file.")

  val CLASS_OPTION = new Option("c", "class", true, "Class with the program entry point (\"main()\" method). Only needed if the JAR file does not specify the class in its manifest.")

  val CLASSPATH_OPTION = new Option("C", "classpath", true, "Adds a URL to each user code classloader  on all nodes in the cluster. The paths must specify a protocol (e.g. file://) and be accessible on all nodes (e.g. by means of a NFS share). You can use this option multiple times for specifying more than one URL. The protocol must be supported by the {@link java.net.URLClassLoader}.")

  val PARALLELISM_OPTION = new Option("p", "parallelism", true, "The parallelism with which to run the program. Optional flag to override the default value specified in the configuration.")

  val DETACHED_OPTION = new Option("d", "detached", false, "If present, runs the job in detached mode")

  val SHUTDOWN_IF_ATTACHED_OPTION = new Option("sae", "shutdownOnAttachedExit", false, "If the job is submitted in attached mode, perform a best-effort cluster shutdown when the CLI is terminated abruptly, e.g., in response to a user interrupt, such as typing Ctrl + C.")

  /**
   * @deprecated use non-prefixed variant {@link #DETACHED_OPTION} for both YARN and non-YARN deployments
   */
  @deprecated val YARN_DETACHED_OPTION = new Option("yd", "yarndetached", false, "If present, runs the job in detached mode (deprecated; use non-YARN specific option instead)")

  val ARGS_OPTION = new Option("a", "arguments", true, "Program arguments. Arguments can also be added without -a, simply as trailing parameters.")

  val ADDRESS_OPTION = new Option("m", "jobmanager", true, "Address of the JobManager to which to connect. Use this flag to connect to a different JobManager than the one specified in the configuration.")

  val SAVEPOINT_PATH_OPTION = new Option("s", "fromSavepoint", true, "Path to a savepoint to restore the job from (for example hdfs:///flink/savepoint-1537).")

  val SAVEPOINT_ALLOW_NON_RESTORED_OPTION = new Option("n", "allowNonRestoredState", false, "Allow to skip savepoint state that cannot be restored. You need to allow this if you removed an operator from your program that was part of the program when the savepoint was triggered.")

  val SAVEPOINT_DISPOSE_OPTION = new Option("d", "dispose", true, "Path of savepoint to dispose.")

  // list specific options
  val RUNNING_OPTION = new Option("r", "running", false, "Show only running programs and their JobIDs")

  val SCHEDULED_OPTION = new Option("s", "scheduled", false, "Show only scheduled programs and their JobIDs")

  val ALL_OPTION = new Option("a", "all", false, "Show all programs and their JobIDs")

  val ZOOKEEPER_NAMESPACE_OPTION = new Option("z", "zookeeperNamespace", true, "Namespace to create the Zookeeper sub-paths for high availability mode")

  val CANCEL_WITH_SAVEPOINT_OPTION = new Option("s", "withSavepoint", true, "**DEPRECATION WARNING**: Cancelling a job with savepoint is deprecated. Use \"stop\" instead. \n Trigger savepoint and cancel job. The target directory is optional. If no directory is specified, the configured default directory ( state.savepoints.dir ) is used.")

  val STOP_WITH_SAVEPOINT_PATH = new Option("p", "savepointPath", true, "Path to the savepoint (for example hdfs:///flink/savepoint-1537). If no directory is specified, the configured default will be used (\"state.savepoints.dir\").")

  val STOP_AND_DRAIN = new Option("d", "drain", false, "Send MAX_WATERMARK before taking the savepoint and stopping the pipelne.")

  val PY_OPTION = new Option("py", "python", true, "Python script with the program entry point. The dependent resources can be configured with the `--pyFiles` option.")

  val PYFILES_OPTION = new Option("pyfs", "pyFiles", true, "Attach custom python files for job. These files will be added to the PYTHONPATH of both the local client and the remote python UDF worker. The standard python resource file suffixes such as .py/.egg/.zip or directory are all supported. Comma (',') could be used as the separator to specify multiple files (e.g.: --pyFiles file:///tmp/myresource.zip,hdfs:///$namenode_address/myresource2.zip).")

  val PYMODULE_OPTION = new Option("pym", "pyModule", true, "Python module with the program entry point. This option must be used in conjunction with `--pyFiles`.")

  val PYREQUIREMENTS_OPTION = new Option("pyreq", "pyRequirements", true, "Specify a requirements.txt file which defines the third-party dependencies. These dependencies will be installed and added to the PYTHONPATH of the python UDF worker. A directory which contains the installation packages of these dependencies could be specified optionally. Use '#' as the separator if the optional parameter exists (e.g.: --pyRequirements file:///tmp/requirements.txt#file:///tmp/cached_dir).")

  val PYARCHIVE_OPTION = new Option("pyarch", "pyArchives", true, "Add python archive files for job. The archive files will be extracted to the working directory of python UDF worker. Currently only zip-format is supported. For each archive file, a target directory be specified. If the target directory name is specified, the archive file will be extracted to a name can directory with the specified name. Otherwise, the archive file will be extracted to a directory with the same name of the archive file. The files uploaded via this option are accessible via relative path. '#' could be used as the separator of the archive file path and the target directory name. Comma (',') could be used as the separator to specify multiple archive files. This option can be used to upload the virtual environment, the data files used in Python UDF (e.g.: --pyArchives file:///tmp/py37.zip,file:///tmp/data.zip#data --pyExecutable py37.zip/py37/bin/python). The data files could be accessed in Python UDF, e.g.: f = open('data/data.txt', 'r').")

  val PYEXEC_OPTION = new Option("pyexec", "pyExecutable", true, "Specify the path of the python interpreter used to execute the python UDF worker (e.g.: --pyExecutable /usr/local/bin/python3). The python UDF worker depends on Python 3.5+, Apache Beam (version == 2.23.0), Pip (version >= 7.1.0) and SetupTools (version >= 37.0.0). Please ensure that the specified environment meets the above requirements.")

  val DYNAMIC_PROPERTIES = Option.builder("D").argName("property=value").numberOfArgs(2).valueSeparator('=').desc("Allows specifying multiple generic configuration options. The available options can be found at https://ci.apache.org/projects/flink/flink-docs-stable/ops/config.html").build

  @deprecated val EXECUTOR_OPTION = new Option("e", "executor", true, "DEPRECATED: Please use the -t option instead which is also available with the \"Application Mode\".\nThe name of the executor to be used for executing the given job, which is equivalent to the \"execution.target\" config option.")

  val TARGET_OPTION = new Option("t", "target", true, "The deployment target for the given application, which is equivalent to the \"execution.target\" config option. For the \"run\" action the currently available targets are: $getExecutorFactoryNames(). For the \"run-application\" action")

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

  /**
   *
   * @return
   */
  def allOptions: Options = {
    val commOptions = getRunCommandOptions
    val yarnOptions = getYARNOptions
    val resultOptions = new Options
    commOptions.getOptions.foreach(resultOptions.addOption)
    yarnOptions.getOptions.filter(x => !resultOptions.hasOption(x.getOpt)).foreach(resultOptions.addOption)
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
