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
package com.streamxhub.common.conf

import org.apache.commons.cli.{Option, Options}
import scala.collection.JavaConversions._


/**
 * copy from flink(1.9.1) sourceCode
 */
object FlinkOption {


  private[this] val HELP_OPTION = new Option("h", "help", false, "Show the help message for the CLI Frontend or the action.")

  private[this] val JAR_OPTION = new Option("j", "jarfile", true, "Flink program JAR file.")

  private[this] val CLASS_OPTION = new Option("c", "class", true, "Class with the program entry point (\"main()\" method or \"getPlan()\" method). Only needed if the " + "JAR file does not specify the class in its manifest.")

  private[this] val CLASSPATH_OPTION = new Option("C", "classpath", true, "Adds a URL to each user code " + "classloader  on all nodes in the cluster. The paths must specify a protocol (e.g. file://) and be " + "accessible on all nodes (e.g. by means of a NFS share). You can use this option multiple " + "times for specifying more than one URL. The protocol must be supported by the " + "{@link java.net.URLClassLoader}.")

  private[this]  val PARALLELISM_OPTION = new Option("p", "parallelism", true, "The parallelism with which to run the program. Optional flag to override the default value " + "specified in the configuration.")

  private[this] val LOGGING_OPTION = new Option("q", "sysoutLogging", false, "If present, " + "suppress logging output to standard out.")

  private[this]  val DETACHED_OPTION = new Option("d", "detached", false, "If present, runs " + "the job in detached mode")

  private[this]  val SHUTDOWN_IF_ATTACHED_OPTION = new Option("sae", "shutdownOnAttachedExit", false, "If the job is submitted in attached mode, perform a best-effort cluster shutdown " + "when the CLI is terminated abruptly, e.g., in response to a user interrupt, such as typing Ctrl + C.")

  /**
   * @deprecated use non-prefixed variant { @link #DETACHED_OPTION} for both YARN and non-YARN deployments
   */
  @deprecated
  private[this]  val YARN_DETACHED_OPTION = new Option("yd", "yarndetached", false, "If present, runs " + "the job in detached mode (deprecated; use non-YARN specific option instead)")

  private[this] val ARGS_OPTION = new Option("a", "arguments", true, "Program arguments. Arguments can also be added without -a, simply as trailing parameters.")

  private[this]  val SAVEPOINT_PATH_OPTION = new Option("s", "fromSavepoint", true, "Path to a savepoint to restore the job from (for example hdfs:///flink/savepoint-1537).")

  private[this]  val SAVEPOINT_ALLOW_NON_RESTORED_OPTION = new Option("n", "allowNonRestoredState", false, "Allow to skip savepoint state that cannot be restored. " + "You need to allow this if you removed an operator from your " + "program that was part of the program when the savepoint was triggered.")

  private[this] val PY_OPTION = new Option("py", "python", true, "Python script with the program entry point. " + "The dependent resources can be configured with the `--pyFiles` option.")

  private[this] val PYFILES_OPTION = new Option("pyfs", "pyFiles", true, "Attach custom python files for job. " + "Comma can be used as the separator to specify multiple files. " + "The standard python resource file suffixes such as .py/.egg/.zip are all supported." + "(eg: --pyFiles file:///tmp/myresource.zip,hdfs:///$namenode_address/myresource2.zip)")

  private[this] val PYMODULE_OPTION = new Option("pym", "pyModule", true, "Python module with the program entry point. " + "This option must be used in conjunction with `--pyFiles`.")

  def getOptions():Options = {
    val commOptions = getRunCommandOptions
    val yarnOptions = getYARNOptions
    val resultOptions = new Options
    commOptions.getOptions.foreach(x=>resultOptions.addOption(x))
    yarnOptions.getOptions.foreach(x=>resultOptions.addOption(x))
    resultOptions
  }

  private[this] def getRunCommandOptions:Options = {
    var options = buildGeneralOptions(new Options)
    options = getProgramSpecificOptions(options)
    options.addOption(SAVEPOINT_PATH_OPTION)
    options.addOption(SAVEPOINT_ALLOW_NON_RESTORED_OPTION)
  }


  private[this]  def getYARNOptions:Options = {
    val shortPrefix = "y"
    val longPrefix = "yarn"
    //yarn
    val query = new Option(shortPrefix + "q", longPrefix + "query", false, "Display available YARN resources (memory, cores)")
    val applicationId = new Option(shortPrefix + "id", longPrefix + "applicationId", true, "Attach to running YARN session")
    val queue = new Option(shortPrefix + "qu", longPrefix + "queue", true, "Specify YARN queue.")
    val shipPath = new Option(shortPrefix + "t", longPrefix + "ship", true, "Ship files in the specified directory (t for transfer)")
    val flinkJar = new Option(shortPrefix + "j", longPrefix + "jar", true, "Path to Flink jar file")
    val jmMemory = new Option(shortPrefix + "jm", longPrefix + "jobManagerMemory", true, "Memory for JobManager Container with optional unit (default: MB)")
    val tmMemory = new Option(shortPrefix + "tm", longPrefix + "taskManagerMemory", true, "Memory per TaskManager Container with optional unit (default: MB)")
    val slots = new Option(shortPrefix + "s", longPrefix + "slots", true, "Number of slots per TaskManager")
    val dynamicproperties = Option.builder(shortPrefix + "D").argName("property=value").numberOfArgs(2).valueSeparator.desc("use value for given property").build
    val name = new Option(shortPrefix + "nm", longPrefix + "name", true, "Set a custom name for the application on YARN")
    val applicationType = new Option(shortPrefix + "at", longPrefix + "applicationType", true, "Set a custom application type for the application on YARN")
    val zookeeperNamespace = new Option(shortPrefix + "z", longPrefix + "zookeeperNamespace", true, "Namespace to create the Zookeeper sub-paths for high availability mode")
    val nodeLabel = new Option(shortPrefix + "nl", longPrefix + "nodeLabel", true, "Specify YARN node label for the YARN application")
    val help = new Option(shortPrefix + "h", longPrefix + "help", false, "Help for the Yarn session CLI.")

    val options = new Options()
    options.addOption(flinkJar)
    options.addOption(jmMemory)
    options.addOption(tmMemory)
    options.addOption(queue)
    options.addOption(query)
    options.addOption(shipPath)
    options.addOption(slots)
    options.addOption(dynamicproperties)
    options.addOption(DETACHED_OPTION)
    options.addOption(SHUTDOWN_IF_ATTACHED_OPTION)
    options.addOption(YARN_DETACHED_OPTION)
    options.addOption(name)
    options.addOption(applicationId)
    options.addOption(applicationType)
    options.addOption(zookeeperNamespace)
    options.addOption(nodeLabel)
    options.addOption(help)
    options
  }

  private[this] def buildGeneralOptions(options: Options) = {
    options.addOption(HELP_OPTION)
    // backwards compatibility: ignore verbose flag (-v)
    options.addOption(new Option("v", "verbose", false, "This option is deprecated."))
    options
  }

  private[this] def getProgramSpecificOptions(options: Options) = {
    options.addOption(JAR_OPTION)
    options.addOption(CLASS_OPTION)
    options.addOption(CLASSPATH_OPTION)
    options.addOption(PARALLELISM_OPTION)
    options.addOption(ARGS_OPTION)
    options.addOption(LOGGING_OPTION)
    options.addOption(DETACHED_OPTION)
    options.addOption(SHUTDOWN_IF_ATTACHED_OPTION)
    options.addOption(YARN_DETACHED_OPTION)
    options.addOption(PY_OPTION)
    options.addOption(PYFILES_OPTION)
    options.addOption(PYMODULE_OPTION)
    options
  }
}
